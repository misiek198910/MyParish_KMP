import secrets
import string
import socket
import os
import urllib3.util.connection as urllib3_cn
from datetime import datetime
from decimal import Decimal
from typing import Optional

import asyncio
import mysql.connector
from collections import defaultdict
import firebase_admin
from firebase_admin import credentials, messaging

from fastapi import FastAPI, Query, Request, Form, HTTPException, BackgroundTasks
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import HTMLResponse, JSONResponse
from fastapi.staticfiles import StaticFiles
from fastapi.templating import Jinja2Templates

urllib3_cn.allowed_gai_family = lambda: socket.AF_INET

# Timeout dla socketów, żeby serwer nie wisiał przy zaciętych połączeniach
socket.setdefaulttimeout(30)

app = FastAPI(title="Moja Parafia - Admin & Priest Hub")

# Port 8004 dla mivs-parishhub - obsługa CORS
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_methods=["*"], allow_headers=["*"])

# --- SANITIZED CREDENTIALS ---
ADMIN_TOKEN = "YOUR_ADMIN_TOKEN_HERE"
ADMIN_PIN = "YOUR_ADMIN_PIN_HERE"

# Inicjalizacja Firebase (tylko jeśli nie jest zainicjowany)
try:
    firebase_admin.get_app()
except ValueError:
    cred = credentials.Certificate("/home/mivs/mivs-parishhub/firebase_key.json")
    firebase_admin.initialize_app(cred)

# --- SANITIZED DATABASE CONFIG ---
db_config = {
    "host": "YOUR_DATABASE_HOST",
    "user": "YOUR_DATABASE_USER",
    "password": "YOUR_DATABASE_PASSWORD",
    "database": "YOUR_DATABASE_NAME"
}

app.mount("/static", StaticFiles(directory="/home/mivs/mivs-parishhub/static"), name="static")

templates = Jinja2Templates(directory=[
    "/home/mivs/mivs-parishhub",
    "/home/mivs/mivs-parish-html",
    "/home/mivs/mivs-html"
])

def increment_html_visitors():
    try:
        conn = mysql.connector.connect(**db_config)
        cursor = conn.cursor()
        cursor.execute("UPDATE server_stats SET html_visitors = html_visitors + 1 WHERE id = 1")
        conn.commit()
        cursor.close()
        conn.close()
    except Exception as e:
        print(f"Błąd licznika HTML: {e}")

def send_parish_update_notification(parish_id, parish_name):
    """Wysyła powiadomienie do osób śledzących parafię (do tematu)"""
    title_text = "Twoja Parafia"
    body_text = f"Właśnie dodaliśmy nowe informacje o parafii {parish_name}."
    
    try:
        topic = f"parish_{parish_id}"
        message = messaging.Message(
            notification=messaging.Notification(
                title=title_text,
                body=body_text
            ),
            data={
                "title": title_text,
                "body": body_text,
                "parish_id": str(parish_id),
                "action": "open_parish"
            },
            apns=messaging.APNSConfig(
                headers={
                    "apns-priority": "10",
                },
                payload=messaging.APNSPayload(
                    aps=messaging.Aps(
                        sound="default",
                        badge=1
                    )
                )
            ),
            topic=topic
        )
        messaging.send(message)
        print(f"DEBUG: Wysłano powiadomienie dla {parish_name}", flush=True)
    except Exception as e:
        print(f"Błąd FCM: {e}", flush=True)

@app.get("/status")
def get_status():
    return {"status": "online", "service": "parishhub"}
  
@app.post("/api/track-visit")
async def track_visit(background_tasks: BackgroundTasks):
    background_tasks.add_task(increment_html_visitors)
    return {"status": "ok"}

@app.get("/panel/{token}", response_class=HTMLResponse)
async def login_to_parish_panel(request: Request, token: str):
    return templates.TemplateResponse("login_panel.html", {"request": request, "token": token})

@app.post("/panel/{token}/verify", response_class=HTMLResponse)
async def verify_priest_pin(request: Request, token: str, pin: str = Form(...)):
    conn = None
    try:
        conn = mysql.connector.connect(**db_config)
        cursor = conn.cursor(dictionary=True)
        
        cursor.execute("SELECT * FROM parishes WHERE access_token = %s", (token,))
        parish = cursor.fetchone()

        if not parish:
            cursor.close()
            return HTMLResponse("<h1>Błąd: Nie znaleziono parafii.</h1>", status_code=404)
            
        if str(parish['pin_code']) != str(pin):
            cursor.close()
            return HTMLResponse("<h1>Błąd: Nieprawidłowy PIN.</h1>", status_code=403)
          
        cursor.execute(
            "SELECT * FROM parish_events WHERE parish_id = %s AND event_date >= DATE_SUB(NOW(), INTERVAL 1 DAY) ORDER BY event_date ASC",
            (parish['id'],)
        )
        events_list = cursor.fetchall()
        
        cursor.close()

        for ev in events_list:
            if isinstance(ev['event_date'], datetime):
                ev['event_date'] = ev['event_date'].strftime('%d.%m.%Y %H:%M')

        sub_exp = parish.get('subscription_expires')
        sub_exp_str = sub_exp.strftime('%d.%m.%Y %H:%M') if sub_exp else "Bezterminowo"
        
        is_active = True if not sub_exp or sub_exp > datetime.now() else False

        lu = parish.get('last_update')
        last_update_str = lu.strftime('%d.%m.%Y %H:%M') if lu else "Brak"

        return templates.TemplateResponse("priest_panel.html", {
            "request": request,
            "token": token,
            "pin": pin,
            "parish_id": parish['id'],
            "parish_name": parish['name'],
            "content": parish['announcements'] or '',
            "intentions": parish['intentions'] or '',
            "bank_acc": parish['bankAccountNumber'] or '',
            "blik_number": parish.get('blikNumber') or '',
            "don_info": parish['donationInfo'] or '',
            "last_update": last_update_str,
            "sub_expires": sub_exp_str,
            "is_sub_active": is_active,
            "events": events_list,
            "photo_url": parish.get('photoUrl') or ''
        })

    except Exception as e:
        print(f"BŁĄD PANELU: {e}")
        return HTMLResponse(f"<h1>Błąd serwera</h1><p>{e}</p>", status_code=500)
        
    finally:
        if conn and conn.is_connected():
            conn.close()

@app.post("/panel/{token}/update")
async def update_parish_data(
    token: str,
    background_tasks: BackgroundTasks,
    pin: str = Form(...),
    announcements: str = Form(""),
    intentions: str = Form(""),
    bankAccountNumber: Optional[str] = Form(None),
    blikNumber: Optional[str] = Form(None),
    donationInfo: Optional[str] = Form(None)
):
    conn = None
    try:
        conn = mysql.connector.connect(**db_config)
        cursor = conn.cursor(dictionary=True)

        cursor.execute("SELECT id, name, pin_code FROM parishes WHERE access_token = %s", (token,))
        parish = cursor.fetchone()

        if not parish:
            print(f"DEBUG: Nie znaleziono parafii dla tokenu: {token}")
            return HTMLResponse("<h1>Błąd: Nieprawidłowy token dostępu.</h1>", status_code=403)

        if str(parish["pin_code"]) != str(pin):
            print(f"DEBUG: Błędny PIN dla parafii {parish['name']}")
            return HTMLResponse("<h1>Błąd: Nieprawidłowy PIN. Sesja wygasła?</h1>", status_code=403)

        def empty_to_none(v):
            return v if v and v.strip() != "" else None

        now = datetime.now()
        parish_id = parish["id"]

        query = """
            UPDATE parishes 
            SET announcements = %s, 
                intentions = %s,
                bankAccountNumber = %s, 
                blikNumber = %s, 
                donationInfo = %s, 
                last_update = %s
            WHERE id = %s
        """
        
        params = (announcements, intentions, empty_to_none(bankAccountNumber), empty_to_none(blikNumber), empty_to_none(donationInfo), now, parish_id)
        
        cursor.execute(query, params)
        conn.commit()
        
        print(f"DEBUG: Sukces! Zaktualizowano parafię {parish['name']} (ID: {parish_id})")

        background_tasks.add_task(send_parish_update_notification, parish_id, parish["name"])

        return HTMLResponse(content=f"""
            <script>
                alert('Ogłoszenia zostały opublikowane!');
                window.location.href = '/panel/{token}';
            </script>
        """)

    except mysql.connector.Error as err:
        print(f"BŁĄD BAZY DANYCH: {err}")
        return HTMLResponse(f"<h1>Błąd bazy danych</h1><p>{err}</p>", status_code=500)
    
    except Exception as e:
        print(f"BŁĄD KRYTYCZNY API: {e}")
        import traceback
        traceback.print_exc()
        return HTMLResponse(f"<h1>Błąd serwera</h1><p>{e}</p>", status_code=500)
    
    finally:
        if conn and conn.is_connected():
            cursor.close()
            conn.close()


@app.get("/mivs-admin/{token}", response_class=HTMLResponse)
async def admin_login_page(request: Request, token: str):
    if token != ADMIN_TOKEN:
        raise HTTPException(status_code=403, detail="Błędny token dostępu")
    return templates.TemplateResponse("admin_login.html", {"request": request, "token": token})

@app.get("/mivs-admin/{token}/{pin}", response_class=HTMLResponse)
async def super_admin_hub(request: Request, token: str, pin: str):
    if token != ADMIN_TOKEN or pin != ADMIN_PIN:
        raise HTTPException(status_code=403)
    
    conn = None
    try:
        conn = mysql.connector.connect(**db_config)
        cursor = conn.cursor(dictionary=True)
        
        cursor.execute("SELECT COUNT(*) as cnt FROM proposals")
        p1 = cursor.fetchone()['cnt']
        cursor.execute("SELECT COUNT(*) as cnt FROM new_parish")
        p2 = cursor.fetchone()['cnt']
        
        cursor.execute("SELECT download_count, html_visitors FROM server_stats WHERE id = 1")
        stats_res = cursor.fetchone()
        downloads = stats_res['download_count'] if stats_res else 0
        html_visitors = stats_res['html_visitors'] if stats_res and stats_res['html_visitors'] is not None else 0
        
        cursor.execute("SELECT COUNT(*) as cnt FROM parishes WHERE subscription_expires > NOW()")
        active_subs = cursor.fetchone()['cnt']
        
        cursor.execute("""
            SELECT COUNT(DISTINCT f.parish_id) as total 
            FROM favorite_logs f
            INNER JOIN parishes p ON f.parish_id = p.id COLLATE utf8mb4_general_ci
        """)
        fav_res = cursor.fetchone()
        fav_count = fav_res['total'] if fav_res and fav_res['total'] is not None else 0
        
        cursor.execute("SELECT id, name, last_update FROM parishes ORDER BY last_update DESC LIMIT 5")
        last_changes = cursor.fetchall()
        for c in last_changes:
            if isinstance(c['last_update'], datetime):
                c['last_update'] = c['last_update'].strftime('%Y-%m-%d %H:%M:%S')

        cursor.close()
        
        return templates.TemplateResponse("admin_hub.html", {
            "request": request,
            "token": token,
            "pin": pin,
            "downloads": downloads,
            "html_visitors": html_visitors,
            "pending": p1 + p2,
            "active_subscriptions": active_subs,
            "total_favorites_count": fav_count,
            "last_changes": last_changes
        })
    except Exception as e:
        print(f"Błąd Hubu: {e}")
        return HTMLResponse(f"<h1>Błąd Krytyczny Hubu</h1><p>{e}</p>")
    finally:
        if conn and conn.is_connected():
            conn.close()
            
@app.get("/api/admin/collaboration-leads")
async def get_collaboration_leads(token: str = Query(None)):
    if token != ADMIN_TOKEN: raise HTTPException(status_code=403)
    conn = None
    try:
        conn = mysql.connector.connect(**db_config)
        cursor = conn.cursor(dictionary=True)
        query = """
            SELECT 
                c.id, 
                c.parish_id, 
                p.name as parish_name, 
                c.email, 
                c.created_at 
            FROM priest_collaborations c
            LEFT JOIN parishes p ON c.parish_id = p.id
            ORDER BY c.created_at DESC
        """
        cursor.execute(query)
        res = cursor.fetchall()
        for r in res:
            if isinstance(r['created_at'], datetime):
                r['created_at'] = r['created_at'].strftime('%d.%m %H:%M')
        return res
    except Exception as e:
        print(f"BŁĄD pobierania zgłoszeń współpracy: {e}")
        return []
    finally:
        if conn: conn.close()

@app.delete("/api/admin/delete-collaboration-lead")
async def delete_collaboration_lead(lead_id: int = Query(...), token: str = Query(None)):
    if token != ADMIN_TOKEN: raise HTTPException(status_code=403)
    conn = None
    try:
        conn = mysql.connector.connect(**db_config)
        cursor = conn.cursor()
        cursor.execute("DELETE FROM priest_collaborations WHERE id = %s", (lead_id,))
        conn.commit()
        return {"status": "success"}
    except: raise HTTPException(status_code=500)
    finally:
        if conn: conn.close()

@app.get("/mivs-admin/{token}/{pin}/proposals", response_class=HTMLResponse)
async def list_proposals_combined(request: Request, token: str, pin: str):
    if token != ADMIN_TOKEN or pin != ADMIN_PIN: raise HTTPException(status_code=403)
    conn = mysql.connector.connect(**db_config)
    cursor = conn.cursor(dictionary=True)
    
    try:
        cursor.execute("""SELECT p.id, p.parish_id, p.name, pa.name as cur_name, 
                          pa.address as cur_address, 'EDIT' as type, p.author_device_id
                          FROM proposals p LEFT JOIN parishes pa ON p.parish_id = pa.id""")
        edits = cursor.fetchall()
        
        cursor.execute("SELECT id, name, diocese, 'NEW' as type, author_device_id FROM new_parish")
        news = cursor.fetchall()
        
        all_props = edits + news
        
        for p in all_props:
            for k, v in p.items():
                if isinstance(v, datetime): p[k] = v.strftime('%Y-%m-%d %H:%M:%S')

        return templates.TemplateResponse("admin_proposals.html", {
            "request": request, "token": token, "pin": pin, "proposals": all_props
        })
    except Exception as e:
        return HTMLResponse(f"<h1>Błąd serwera</h1><p>{e}</p>")
    finally:
        cursor.close()
        conn.close()

@app.get("/api/admin/proposal-details/{p_type}/{p_id}")
async def get_proposal_details(p_type: str, p_id: int, token: str = Query(None)):
    if token != ADMIN_TOKEN: raise HTTPException(status_code=403)
    conn = mysql.connector.connect(**db_config)
    cursor = conn.cursor(dictionary=True)
    try:
        if p_type == 'NEW':
            cursor.execute("SELECT *, 'NEW' as type FROM new_parish WHERE id = %s", (p_id,))
        else:
            query = """
                SELECT 
                    p.*, p.id as proposal_id, 'EDIT' as type,
                    pa.name as cur_name, pa.address as cur_address, pa.latitude as cur_latitude,
                    pa.longitude as cur_longitude, pa.foundingYear as cur_foundingYear,
                    pa.photoUrl as cur_photoUrl, pa.diocese as cur_diocese, pa.deanery as cur_deanery,
                    pa.pastorName as cur_pastorName, pa.phoneNum as cur_phoneNum, pa.email as cur_email,
                    pa.websiteUrl as cur_websiteUrl, pa.socialMediaFacebook as cur_socialMediaFacebook,
                    pa.socialMediaYouTube as cur_socialMediaYouTube, pa.socialMediaInstagram as cur_socialMediaInstagram,
                    pa.massHoursMonday as cur_massHoursMonday, pa.massHoursTuesday as cur_massHoursTuesday, 
                    pa.massHoursWednesday as cur_massHoursWednesday, pa.massHoursThursday as cur_massHoursThursday,
                    pa.massHoursFriday as cur_massHoursFriday, pa.massHoursSaturday as cur_massHoursSaturday,
                    pa.massHoursSunday as cur_massHoursSunday, pa.hasMassSundayHour as cur_hasMassSundayHour,
                    pa.hasMassForChildrenHour as cur_hasMassForChildrenHour, pa.confessionInfo as cur_confession, 
                    pa.adorationInfo as cur_adoration, pa.officeHoursText as cur_officeHoursText, 
                    pa.last_update as cur_last_update, pa.firstSaturdayOfMonth_hour as cur_first_sat_hr, 
                    pa.firstSaturdayOfMonth_info as cur_first_sat_info
                FROM proposals p 
                LEFT JOIN parishes pa ON p.parish_id = pa.id 
                WHERE p.id = %s
            """
            cursor.execute(query, (p_id,))
        
        res = cursor.fetchone()
        if res:
            for k, v in res.items():
                if isinstance(v, datetime): res[k] = v.strftime('%Y-%m-%d %H:%M:%S')
                elif isinstance(v, Decimal): res[k] = float(v)
        return res
    finally:
        cursor.close()
        conn.close()

@app.post("/mivs-admin/{token}/{pin}/proposals/create/{prop_id}")
async def create_parish_from_proposal(
    token: str, pin: str, prop_id: int, background_tasks: BackgroundTasks,
    id_manual: Optional[str] = Form(None), name: str = Form(...), address: str = Form(""), diocese: str = Form(""),
    deanery: str = Form(""), pastorName: str = Form(""), email: str = Form(""), phoneNum: str = Form(""),
    websiteUrl: str = Form(""), socialMediaFacebook: str = Form(""), socialMediaYouTube: str = Form(""),
    socialMediaInstagram: str = Form(""), latitude: str = Form(""), longitude: str = Form(""), foundingYear: str = Form(""),
    isCathedral: int = Form(0), photoUrl: str = Form(""), access_token: str = Form(""), pin_code: str = Form(""),
    massHoursSunday: str = Form(""), massHoursMonday: str = Form(""), massHoursTuesday: str = Form(""),
    massHoursWednesday: str = Form(""), massHoursThursday: str = Form(""), massHoursFriday: str = Form(""),
    massHoursSaturday: str = Form(""), hasMassSunday: int = Form(0), hasMassSundayHour: str = Form(""),
    hasMassForChildren: int = Form(0), hasMassForChildrenHour: str = Form(""), confessionInfo: str = Form(""),
    adorationInfo: str = Form(""), officeHoursText: str = Form(""), firstSaturdayOfMonth: int = Form(0),
    firstSaturdayOfMonth_hour: str = Form(""), firstSaturdayOfMonth_info: str = Form("")
):
    if token != ADMIN_TOKEN or pin != ADMIN_PIN: raise HTTPException(status_code=403)
    conn = mysql.connector.connect(**db_config)
    cursor = conn.cursor()
    
    sql = """INSERT INTO parishes (
        id, name, address, diocese, deanery, pastorName, email, phoneNum, websiteUrl, 
        socialMediaFacebook, socialMediaYouTube, socialMediaInstagram, photoUrl, latitude, longitude, 
        foundingYear, isCathedral, access_token, pin_code, massHoursSunday, massHoursMonday, massHoursTuesday, 
        massHoursWednesday, massHoursThursday, massHoursFriday, massHoursSaturday, hasMassSunday, hasMassSundayHour, 
        hasMassForChildren, hasMassForChildrenHour, confessionInfo, adorationInfo, officeHoursText, firstSaturdayOfMonth, 
        firstSaturdayOfMonth_hour, firstSaturdayOfMonth_info, last_update, is_active
    ) VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s, NOW(), 1)"""
    
    params = (
        id_manual, name, address, diocese, deanery, pastorName, email, phoneNum, websiteUrl, socialMediaFacebook,
        socialMediaYouTube, socialMediaInstagram, photoUrl, float(latitude) if latitude else None, float(longitude) if longitude else None,
        foundingYear, isCathedral, access_token, pin_code, massHoursSunday, massHoursMonday, massHoursTuesday, massHoursWednesday,
        massHoursThursday, massHoursFriday, massHoursSaturday, hasMassSunday, hasMassSundayHour, hasMassForChildren,
        hasMassForChildrenHour, confessionInfo, adorationInfo, officeHoursText, firstSaturdayOfMonth, firstSaturdayOfMonth_hour, firstSaturdayOfMonth_info
    )
    cursor.execute(sql, params)
    new_id = id_manual if id_manual else cursor.lastrowid
    
    cursor.execute("SELECT author_device_id FROM new_parish WHERE id = %s", (prop_id,))
    author_res = cursor.fetchone()
    if author_res and author_res[0]:
        cursor.execute("""
            INSERT INTO user_profiles (device_id, points) VALUES (%s, 1)
            ON DUPLICATE KEY UPDATE points = COALESCE(points, 0) + 1
        """, (author_res[0],))

    cursor.execute("DELETE FROM new_parish WHERE id = %s", (prop_id,))
    cursor.execute("DELETE FROM proposals WHERE id = %s", (prop_id,))
    
    conn.commit()
    cursor.close()
    conn.close()
    background_tasks.add_task(send_parish_update_notification, new_id, name)
    return {"status": "success"}

@app.post("/mivs-admin/{token}/{pin}/proposals/apply/{prop_id}")
async def apply_proposal(
    token: str, pin: str, prop_id: int, background_tasks: BackgroundTasks,
    parish_id: str = Form(...), name: str = Form(...), address: str = Form(""),
    isCathedral: Optional[int] = Form(0), photoUrl: str = Form(""), last_update: Optional[str] = Form(None),
    hasMassSunday: Optional[int] = Form(0), hasMassForChildren: Optional[int] = Form(0),
    massHoursSunday: str = Form(""), hasMassSundayHour: str = Form(""), hasMassForChildrenHour: str = Form(""),
    massHoursMonday: str = Form(""), massHoursTuesday: str = Form(""), massHoursWednesday: str = Form(""),
    massHoursThursday: str = Form(""), massHoursFriday: str = Form(""), massHoursSaturday: str = Form(""),
    confessionInfo: str = Form(""), adorationInfo: str = Form(""), firstSaturdayOfMonth: Optional[int] = Form(0),
    firstSaturdayOfMonth_hour: str = Form(""), firstSaturdayOfMonth_info: str = Form(""),
    officeHoursText: str = Form(""), phoneNum: str = Form(""), email: str = Form(""), websiteUrl: str = Form(""),
    pastorName: str = Form(""), diocese: str = Form(""), deanery: str = Form(""), foundingYear: str = Form(""),
    socialMediaFacebook: str = Form(""), socialMediaYouTube: str = Form(""), socialMediaInstagram: str = Form(""),
    latitude: str = Form(""), longitude: str = Form(""), is_active: Optional[int] = Form(0)
):
    if token != ADMIN_TOKEN or pin != ADMIN_PIN: raise HTTPException(status_code=403)
    conn = mysql.connector.connect(**db_config)
    cursor = conn.cursor()
    update_time = last_update or datetime.now().strftime('%Y-%m-%d %H:%M:%S')
    
    sql = """UPDATE parishes SET name=%s, address=%s, isCathedral=%s, photoUrl=%s, hasMassSunday=%s, 
             hasMassForChildren=%s, massHoursSunday=%s, hasMassSundayHour=%s, hasMassForChildrenHour=%s, 
             massHoursMonday=%s, massHoursTuesday=%s, massHoursWednesday=%s, massHoursThursday=%s, 
             massHoursFriday=%s, massHoursSaturday=%s, confessionInfo=%s, adorationInfo=%s, 
             officeHoursText=%s, phoneNum=%s, email=%s, websiteUrl=%s, pastorName=%s, diocese=%s, deanery=%s, 
             foundingYear=%s, socialMediaFacebook=%s, socialMediaYouTube=%s, socialMediaInstagram=%s, 
             firstSaturdayOfMonth=%s, firstSaturdayOfMonth_hour=%s, firstSaturdayOfMonth_info=%s, 
             latitude=%s, longitude=%s, is_active=%s, last_update=%s WHERE id=%s"""
    
    params = (name, address, isCathedral, photoUrl, hasMassSunday, hasMassForChildren, massHoursSunday,
              hasMassSundayHour, hasMassForChildrenHour, massHoursMonday, massHoursTuesday, massHoursWednesday,
              massHoursThursday, massHoursFriday, massHoursSaturday, confessionInfo, adorationInfo, officeHoursText,
              phoneNum, email, websiteUrl, pastorName, diocese, deanery, foundingYear, socialMediaFacebook,
              socialMediaYouTube, socialMediaInstagram, firstSaturdayOfMonth, firstSaturdayOfMonth_hour,
              firstSaturdayOfMonth_info, float(latitude) if latitude else None, float(longitude) if longitude else None,
              is_active, update_time, parish_id)
    
    cursor.execute(sql, params)

    cursor.execute("SELECT author_device_id FROM proposals WHERE id = %s", (prop_id,))
    author_res = cursor.fetchone()
    if author_res and author_res[0]:
        cursor.execute("""
            INSERT INTO user_profiles (device_id, points) VALUES (%s, 1)
            ON DUPLICATE KEY UPDATE points = COALESCE(points, 0) + 1
        """, (author_res[0],))

    cursor.execute("DELETE FROM proposals WHERE id=%s", (prop_id,))
    conn.commit()
    cursor.close()
    conn.close()
    background_tasks.add_task(send_parish_update_notification, parish_id, name)
    return {"status": "success"}

@app.post("/mivs-admin/{token}/{pin}/proposals/delete/{p_type}/{prop_id}")
async def delete_proposal(token: str, pin: str, p_type: str, prop_id: int):
    if token != ADMIN_TOKEN or pin != ADMIN_PIN: raise HTTPException(status_code=403)
    
    conn = mysql.connector.connect(**db_config)
    cursor = conn.cursor()
    
    try:
        if p_type == 'NEW':
            cursor.execute("DELETE FROM new_parish WHERE id = %s", (prop_id,))
        else:
            cursor.execute("DELETE FROM proposals WHERE id = %s", (prop_id,))
            
        conn.commit()
        return {"status": "success"}
    except Exception as e:
        return JSONResponse(status_code=500, content={"status": "error", "message": str(e)})
    finally:
        cursor.close()
        conn.close()

@app.get("/api/admin/next-id")
async def get_next_id(token: str = Query(None)):
    if token != ADMIN_TOKEN:
        raise HTTPException(status_code=403, detail="Błędny token")
    
    conn = None
    try:
        conn = mysql.connector.connect(**db_config)
        cursor = conn.cursor()
        cursor.execute("SELECT MAX(CAST(id AS UNSIGNED)) FROM parishes")
        result = cursor.fetchone()
        max_id = result[0] if result and result[0] is not None else 0
        cursor.close()
        return {"next_id": int(max_id) + 1}
    finally:
        if conn: conn.close()

@app.get("/mivs-admin/{token}/{pin}/parishes_manage", response_class=HTMLResponse)
async def list_parishes_manage(request: Request, token: str, pin: str, q: str = Query(None)):
    if token != ADMIN_TOKEN or pin != ADMIN_PIN: raise HTTPException(status_code=403)
    parishes = []
    if q:
        conn = mysql.connector.connect(**db_config)
        cursor = conn.cursor(dictionary=True)
        cursor.execute("SELECT id, name, diocese, access_token FROM parishes WHERE id = %s OR name LIKE %s", (q, f"%{q}%"))
        parishes = cursor.fetchall()
        cursor.close()
        conn.close()
    return templates.TemplateResponse("admin_parish_list.html", {"request": request, "token": token, "pin": pin, "parishes": parishes, "query": q or ""})

@app.get("/mivs-admin/{token}/{pin}/parishes_manage/edit/{parish_id}", response_class=HTMLResponse)
async def edit_parish_form(request: Request, token: str, pin: str, parish_id: str):
    if token != ADMIN_TOKEN or pin != ADMIN_PIN: raise HTTPException(status_code=403)
    parish = None
    
    if parish_id == "new":
        conn = mysql.connector.connect(**db_config)
        cursor = conn.cursor()
        cursor.execute("SELECT MAX(CAST(id AS UNSIGNED)) FROM parishes")
        result = cursor.fetchone()
        max_id = result[0] if result and result[0] is not None else 0
        cursor.close()
        conn.close()
        
        alphabet = string.ascii_lowercase + string.digits
        auto_token = ''.join(secrets.choice(alphabet) for _ in range(12))
        auto_pin = secrets.randbelow(9000) + 1000
        
        parish = {
            "id": int(max_id) + 1,
            "name": "",
            "address": "",
            "diocese": "",
            "access_token": auto_token,
            "pin_code": auto_pin,
            "is_new": True
        }
    else:
        conn = mysql.connector.connect(**db_config)
        cursor = conn.cursor(dictionary=True)
        cursor.execute("SELECT * FROM parishes WHERE id = %s", (parish_id,))
        parish = cursor.fetchone()
        if parish:
            parish["is_new"] = False
            for k, v in parish.items():
                if isinstance(v, datetime): parish[k] = v.strftime('%Y-%m-%d %H:%M:%S')
                elif isinstance(v, Decimal): parish[k] = float(v)
        cursor.close()
        conn.close()
        
    return templates.TemplateResponse("admin_parish_edit.html", {"request": request, "token": token, "pin": pin, "p": parish})

@app.post("/mivs-admin/{token}/{pin}/parishes_manage/create")
async def create_parish_manual(
    token: str, pin: str, background_tasks: BackgroundTasks,
    id_manual: str = Form(...), name: str = Form(...), address: str = Form(""),
    foundingYear: str = Form(""), latitude: str = Form(""), longitude: str = Form(""),
    isCathedral: int = Form(0), photoUrl: str = Form(""), diocese: str = Form(""),
    deanery: str = Form(""), pastorName: str = Form(""), phoneNum: str = Form(""),
    email: str = Form(""), websiteUrl: str = Form(""), socialMediaFacebook: str = Form(""),
    socialMediaYouTube: str = Form(""), socialMediaInstagram: str = Form(""),
    hasMassSunday: int = Form(0), hasMassSundayHour: str = Form(""),
    hasMassForChildren: int = Form(0), hasMassForChildrenHour: str = Form(""),
    massHoursMonday: str = Form(""), massHoursTuesday: str = Form(""),
    massHoursWednesday: str = Form(""), massHoursThursday: str = Form(""),
    massHoursFriday: str = Form(""), massHoursSaturday: str = Form(""),
    massHoursSunday: str = Form(""), firstSaturdayOfMonth: int = Form(0),
    firstSaturdayOfMonth_hour: str = Form(""), firstSaturdayOfMonth_info: str = Form(""),
    adorationInfo: str = Form(""), confessionInfo: str = Form(""),
    officeHoursText: str = Form(""), announcements: str = Form(""),
    bankAccountNumber: str = Form(""), donationInfo: str = Form(""),
    access_token: str = Form(""), pin_code: str = Form(""),
    first_subscription_date: str = Form(""), last_subscription_date: str = Form(""),
    subscription_expires: str = Form(""), is_active: int = Form(0)
):
    if token != ADMIN_TOKEN or pin != ADMIN_PIN: raise HTTPException(status_code=403)
    conn = mysql.connector.connect(**db_config)
    cursor = conn.cursor()
    
    def e_n(v): return v if v and v.strip() != "" else None
    
    try:
        sql = """INSERT INTO parishes (id, name, address, foundingYear, latitude, longitude, isCathedral, photoUrl, 
                diocese, deanery, pastorName, phoneNum, email, websiteUrl, socialMediaFacebook, socialMediaYouTube, 
                socialMediaInstagram, hasMassSunday, hasMassSundayHour, hasMassForChildren, hasMassForChildrenHour, 
                massHoursMonday, massHoursTuesday, massHoursWednesday, massHoursThursday, massHoursFriday, 
                massHoursSaturday, massHoursSunday, firstSaturdayOfMonth, firstSaturdayOfMonth_hour, 
                firstSaturdayOfMonth_info, adorationInfo, confessionInfo, officeHoursText, announcements, 
                bankAccountNumber, donationInfo, access_token, pin_code, first_subscription_date, 
                last_subscription_date, subscription_expires, is_active, last_update) 
                VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s, NOW())"""
        
        params = (id_manual, name, address, e_n(foundingYear),
                  float(latitude) if latitude else None, float(longitude) if longitude else None,
                  isCathedral, photoUrl, diocese, deanery, pastorName, phoneNum, email, websiteUrl,
                  socialMediaFacebook, socialMediaYouTube, socialMediaInstagram, hasMassSunday,
                  hasMassSundayHour, hasMassForChildren, hasMassForChildrenHour, massHoursMonday,
                  massHoursTuesday, massHoursWednesday, massHoursThursday, massHoursFriday,
                  massHoursSaturday, massHoursSunday, firstSaturdayOfMonth, firstSaturdayOfMonth_hour,
                  firstSaturdayOfMonth_info, adorationInfo, confessionInfo, officeHoursText, announcements,
                  bankAccountNumber, donationInfo, access_token, pin_code, e_n(first_subscription_date),
                  e_n(last_subscription_date), e_n(subscription_expires), is_active)
        
        cursor.execute(sql, params)
        conn.commit()
        background_tasks.add_task(send_parish_update_notification, id_manual, name)
        return {"status": "success"}
    except Exception as e:
        return JSONResponse(status_code=500, content={"status": "error", "message": str(e)})
    finally:
        cursor.close()
        conn.close()

@app.post("/mivs-admin/{token}/{pin}/parishes_manage/update/{parish_id}")
async def update_parish_full(
    token: str, pin: str, parish_id: str, background_tasks: BackgroundTasks,
    name: str = Form(...), address: str = Form(""), foundingYear: str = Form(""),
    latitude: str = Form(""), longitude: str = Form(""), isCathedral: int = Form(0),
    photoUrl: str = Form(""), diocese: str = Form(""), deanery: str = Form(""),
    pastorName: str = Form(""), phoneNum: str = Form(""), email: str = Form(""),
    websiteUrl: str = Form(""), socialMediaFacebook: str = Form(""),
    socialMediaYouTube: str = Form(""), socialMediaInstagram: str = Form(""),
    hasMassSunday: int = Form(0), hasMassSundayHour: str = Form(""),
    hasMassForChildren: int = Form(0), hasMassForChildrenHour: str = Form(""),
    massHoursMonday: str = Form(""), massHoursTuesday: str = Form(""),
    massHoursWednesday: str = Form(""), massHoursThursday: str = Form(""),
    massHoursFriday: str = Form(""), massHoursSaturday: str = Form(""),
    massHoursSunday: str = Form(""), firstSaturdayOfMonth: int = Form(0),
    firstSaturdayOfMonth_hour: str = Form(""), firstSaturdayOfMonth_info: str = Form(""),
    adorationInfo: str = Form(""), confessionInfo: str = Form(""),
    officeHoursText: str = Form(""), announcements: str = Form(""),
    bankAccountNumber: str = Form(""), donationInfo: str = Form(""),
    access_token: str = Form(""), pin_code: str = Form(""),
    first_subscription_date: str = Form(""), last_subscription_date: str = Form(""),
    subscription_expires: str = Form(""), is_active: int = Form(0),
    last_update: str = Form("")
):
    if token != ADMIN_TOKEN or pin != ADMIN_PIN: raise HTTPException(status_code=403)
    
    conn = mysql.connector.connect(**db_config)
    cursor = conn.cursor()
    
    def empty_to_none(v): return v if v and v.strip() != "" else None
    
    try:
        sql = """UPDATE parishes SET 
            name=%s, address=%s, foundingYear=%s, latitude=%s, longitude=%s, 
            isCathedral=%s, photoUrl=%s, diocese=%s, deanery=%s, pastorName=%s, 
            phoneNum=%s, email=%s, websiteUrl=%s, socialMediaFacebook=%s, 
            socialMediaYouTube=%s, socialMediaInstagram=%s, hasMassSunday=%s, 
            hasMassSundayHour=%s, hasMassForChildren=%s, hasMassForChildrenHour=%s, 
            massHoursMonday=%s, massHoursTuesday=%s, massHoursWednesday=%s, 
            massHoursThursday=%s, massHoursFriday=%s, massHoursSaturday=%s, 
            massHoursSunday=%s, firstSaturdayOfMonth=%s, firstSaturdayOfMonth_hour=%s, 
            firstSaturdayOfMonth_info=%s, adorationInfo=%s, confessionInfo=%s, 
            officeHoursText=%s, announcements=%s, bankAccountNumber=%s, 
            donationInfo=%s, access_token=%s, pin_code=%s, first_subscription_date=%s, 
            last_subscription_date=%s, subscription_expires=%s, is_active=%s, 
            last_update=%s 
            WHERE id=%s"""
            
        params = (
            name, address, empty_to_none(foundingYear),
            float(latitude) if latitude and latitude.strip() else None,
            float(longitude) if longitude and longitude.strip() else None,
            isCathedral, photoUrl, diocese, deanery, pastorName,
            phoneNum, email, websiteUrl, socialMediaFacebook,
            socialMediaYouTube, socialMediaInstagram, hasMassSunday,
            hasMassSundayHour, hasMassForChildren, hasMassForChildrenHour,
            massHoursMonday, massHoursTuesday, massHoursWednesday,
            massHoursThursday, massHoursFriday, massHoursSaturday,
            massHoursSunday, firstSaturdayOfMonth, firstSaturdayOfMonth_hour,
            firstSaturdayOfMonth_info, adorationInfo, confessionInfo,
            officeHoursText, announcements, bankAccountNumber,
            donationInfo, access_token, pin_code,
            empty_to_none(first_subscription_date), empty_to_none(last_subscription_date),
            empty_to_none(subscription_expires), is_active,
            empty_to_none(last_update) or datetime.now().strftime('%Y-%m-%d %H:%M:%S'),
            parish_id
        )
        
        cursor.execute(sql, params)
        conn.commit()
        background_tasks.add_task(send_parish_update_notification, parish_id, name)
        return {"status": "success"}
    except Exception as e:
        return JSONResponse(status_code=500, content={"status": "error", "message": str(e)})
    finally:
        cursor.close()
        conn.close()

@app.post("/mivs-admin/{token}/{pin}/parishes_manage/delete/{parish_id}")
async def delete_parish_permanently(token: str, pin: str, parish_id: str):
    if token != ADMIN_TOKEN or pin != ADMIN_PIN: raise HTTPException(status_code=403)
    conn = mysql.connector.connect(**db_config)
    cursor = conn.cursor()
    try:
        cursor.execute("DELETE FROM parishes WHERE id = %s", (parish_id,))
        conn.commit()
        return {"status": "success"}
    except Exception as e:
        return JSONResponse(status_code=500, content={"status": "error", "message": str(e)})
    finally:
        cursor.close()
        conn.close()

@app.get("/mivs-admin/{token}/{pin}/subscriptions", response_class=HTMLResponse)
async def list_subscriptions(request: Request, token: str, pin: str, q: Optional[str] = Query(None)):
    if token != ADMIN_TOKEN or pin != ADMIN_PIN: raise HTTPException(status_code=403)
    conn = mysql.connector.connect(**db_config)
    cursor = conn.cursor(dictionary=True)
    parish = None

    try:
        if q and q.strip():
            cursor.execute("""
                SELECT id, name, subscription_expires, is_active 
                FROM parishes 
                WHERE id = %s OR name LIKE %s 
                LIMIT 1
            """, (q, f"%{q}%"))
            parish = cursor.fetchone()
            if parish and isinstance(parish['subscription_expires'], datetime):
                parish['subscription_expires'] = parish['subscription_expires'].strftime('%Y-%m-%d %H:%M:%S')

        return templates.TemplateResponse("admin_subscriptions.html", {
            "request": request, "token": token, "pin": pin, "p": parish, "query": q or ""
        })
    finally:
        cursor.close()
        conn.close()

@app.post("/api/admin/extend-subscription/{parish_id}")
async def extend_subscription(parish_id: str, token: str = Query(None)):
    if token != ADMIN_TOKEN: raise HTTPException(status_code=403)
    conn = mysql.connector.connect(**db_config)
    cursor = conn.cursor()
    try:
        sql = """
            UPDATE parishes 
            SET subscription_expires = IF(subscription_expires > NOW(), 
                                         DATE_ADD(subscription_expires, INTERVAL 30 DAY), 
                                         DATE_ADD(NOW(), INTERVAL 30 DAY)),
                is_active = 1, last_update = NOW()
            WHERE id = %s
        """
        cursor.execute(sql, (parish_id,))
        conn.commit()
        return {"status": "success"}
    except Exception as e:
        return JSONResponse(status_code=500, content={"status": "error", "message": str(e)})
    finally:
        cursor.close()
        conn.close()
        
@app.post("/api/admin/disable-subscription/{parish_id}")
async def disable_subscription(parish_id: str, token: str = Query(None)):
    if token != ADMIN_TOKEN: raise HTTPException(status_code=403)
    conn = None
    try:
        conn = mysql.connector.connect(**db_config)
        cursor = conn.cursor()
        
        sql = """
            UPDATE parishes 
            SET is_active = 0, 
                last_update = NOW() 
            WHERE id = %s
        """
        cursor.execute(sql, (parish_id,))
        conn.commit()
        return {"status": "success"}
    except Exception as e:
        return JSONResponse(status_code=500, content={"status": "error", "message": str(e)})
    finally:
        if conn: conn.close()

@app.get("/api/admin/favorites-summary")
async def get_favorites_summary(token: str = Query(None)):
    if token != ADMIN_TOKEN: raise HTTPException(status_code=403)
    conn = mysql.connector.connect(**db_config)
    cursor = conn.cursor(dictionary=True)
    try:
        query = """
            SELECT 
                f.parish_id as id, 
                p.name as name, 
                COUNT(f.parish_id) as total_count 
            FROM favorite_logs f
            INNER JOIN parishes p ON f.parish_id = p.id COLLATE utf8mb4_general_ci
            GROUP BY f.parish_id, p.name
            ORDER BY total_count DESC
        """
        cursor.execute(query)
        res = cursor.fetchall()
        return res
    except Exception as e:
        print(f"BŁĄD SQL w rankingu: {e}", flush=True)
        return []
    finally:
        cursor.close()
        conn.close()

@app.delete("/api/admin/delete-favorite-logs")
async def delete_favorite_logs(parish_id: str = Query(...), token: str = Query(None)):
    if token != ADMIN_TOKEN: raise HTTPException(status_code=403)
    try:
        conn = mysql.connector.connect(**db_config)
        cursor = conn.cursor()
        cursor.execute("DELETE FROM favorite_logs WHERE parish_id = %s", (parish_id,))
        conn.commit()
        cursor.close()
        conn.close()
        return {"status": "success"}
    except: raise HTTPException(status_code=500)

@app.post("/favorites/log")
async def log_favorite_action(parish_id: str = Form(...), parish_name: str = Form(...), action: str = Form(...)):
    try:
        conn = mysql.connector.connect(**db_config)
        cursor = conn.cursor()
        cursor.execute("INSERT INTO favorite_logs (parish_id, parish_name, action) VALUES (%s, %s, %s)", (parish_id, parish_name, action))
        conn.commit()
        cursor.close()
        conn.close()
        return {"status": "success"}
    except: raise HTTPException(status_code=500)
    
@app.get("/api/admin/active-subscriptions-list")
async def get_active_subscriptions_list(token: str = Query(None)):
    if token != ADMIN_TOKEN: raise HTTPException(status_code=403)
    conn = None
    try:
        conn = mysql.connector.connect(**db_config)
        cursor = conn.cursor(dictionary=True)
        query = """
            SELECT id, name, subscription_expires 
            FROM parishes 
            WHERE subscription_expires > NOW()
            ORDER BY subscription_expires ASC
        """
        cursor.execute(query)
        res = cursor.fetchall()
        for r in res:
            if isinstance(r['subscription_expires'], datetime):
                # Formatujemy: Dzień.Miesiąc.Rok Godzina:Minuta
                r['subscription_expires'] = r['subscription_expires'].strftime('%d.%m.%Y %H:%M')
        return res
    except Exception as e:
        print(f"BŁĄD pobierania aktywnych subskrypcji: {e}")
        return []
    finally:
        if conn: conn.close()
        
        

@app.get("/mivs-admin/{token}/{pin}/intentions", response_class=HTMLResponse)
async def manage_intentions(request: Request, token: str, pin: str):
    if token != ADMIN_TOKEN or pin != ADMIN_PIN: raise HTTPException(status_code=403)
    
    conn = mysql.connector.connect(**db_config)
    cursor = conn.cursor(dictionary=True)
    try:
        cursor.execute("SELECT id FROM parish_intentions WHERE expires_at < NOW()")
        expired_intentions = cursor.fetchall()
        
        if expired_intentions:
            expired_ids = [str(row['id']) for row in expired_intentions]
            ids_string = ",".join(expired_ids)
            
            cursor.execute(f"DELETE FROM parish_intention_prayers WHERE intention_id IN ({ids_string})")
            cursor.execute(f"DELETE FROM intention_candles WHERE intention_id IN ({ids_string})")
            cursor.execute(f"DELETE FROM parish_intentions WHERE id IN ({ids_string})")
            conn.commit()

        cursor.execute("SELECT id, name FROM parishes ORDER BY name ASC")
        parishes = cursor.fetchall()
        
        cursor.execute("""
            SELECT ic.id, ic.intention_id, ic.device_id, ic.candle_type, ic.created_at, ic.expires_at,
                   p.name as lighter_parish_name
            FROM intention_candles ic
            LEFT JOIN user_profiles up ON ic.device_id = up.device_id
            LEFT JOIN parishes p ON up.home_parish_id = p.id
            ORDER BY ic.created_at ASC
        """)
        all_candles = cursor.fetchall()
        
        candles_by_intention = defaultdict(list)
        for c in all_candles:
            if isinstance(c['created_at'], datetime):
                c['created_at'] = c['created_at'].strftime('%Y-%m-%d %H:%M')
            if isinstance(c['expires_at'], datetime):
                c['expires_at'] = c['expires_at'].strftime('%Y-%m-%d %H:%M')
            candles_by_intention[c['intention_id']].append(c)

        cursor.execute("SELECT admin_device_id FROM admin_id WHERE id = 1")
        admin_data = cursor.fetchone()
        admin_device_id = admin_data['admin_device_id'] if admin_data and admin_data['admin_device_id'] else 'ADMIN_HUB'

        cursor.execute("""
            SELECT i.id, i.author_parish_id as parish_id, p.name as parish_name, 
                   i.content, i.created_at, i.category, i.is_anonymous, i.is_pinned,
                   IF(pip.user_device_id IS NOT NULL, 1, 0) as prayed_by_admin
            FROM parish_intentions i
            LEFT JOIN parishes p ON i.author_parish_id = p.id
            LEFT JOIN parish_intention_prayers pip ON i.id = pip.intention_id AND pip.user_device_id = %s
            ORDER BY i.is_pinned DESC, i.created_at DESC
        """, (admin_device_id,))
        intentions = cursor.fetchall()
        
        for i in intentions:
            if isinstance(i['created_at'], datetime):
                i['created_at'] = i['created_at'].strftime('%Y-%m-%d %H:%M')
            i['candles'] = candles_by_intention.get(i['id'], [])
                
        return templates.TemplateResponse("admin_intentions.html", {
            "request": request,
            "token": token,
            "pin": pin,
            "parishes": parishes,
            "intentions": intentions
        })
    except Exception as e:
        return HTMLResponse(f"<h1>Błąd serwera</h1><p>{e}</p>")
    finally:
        cursor.close()
        conn.close()

@app.post("/api/admin/intentions/create")
async def create_intention(
    parish_id: str = Form(...),
    content: str = Form(...),
    category: str = Form("Ogólna"),
    is_anonymous: int = Form(0),
    token: str = Query(None)
):
    if token != ADMIN_TOKEN: raise HTTPException(status_code=403)
    conn = mysql.connector.connect(**db_config)
    cursor = conn.cursor(dictionary=True)
    try:
        from datetime import datetime, timedelta
        expires_at = datetime.now() + timedelta(days=7)
        
        cursor.execute("SELECT admin_device_id FROM admin_id WHERE id = 1")
        admin_data = cursor.fetchone()
        
        my_device_id = admin_data['admin_device_id'] if admin_data and admin_data['admin_device_id'] else 'ADMIN_HUB'
        
        cursor.execute(
            """INSERT INTO parish_intentions 
               (author_parish_id, content, category, is_anonymous, created_at, expires_at, author_device_id) 
               VALUES (%s, %s, %s, %s, NOW(), %s, %s)""",
            (parish_id, content, category, is_anonymous, expires_at, my_device_id)
        )
        
        cursor.execute("""
            UPDATE parishes 
            SET active_intentions = active_intentions + 1, last_update = NOW() 
            WHERE id = %s
        """, (parish_id,))
        
        conn.commit()
        return {"status": "success"}
    except Exception as e:
        if conn: conn.rollback()
        return JSONResponse(status_code=500, content={"status": "error", "message": str(e)})
    finally:
        cursor.close()
        conn.close()

@app.post("/api/admin/intentions/update/{intention_id}")
async def update_intention(
    intention_id: int,
    parish_id: str = Form(...),
    content: str = Form(...),
    category: str = Form("Ogólna"),
    is_anonymous: int = Form(0),
    token: str = Query(None)
):
    if token != ADMIN_TOKEN: raise HTTPException(status_code=403)
    conn = mysql.connector.connect(**db_config)
    cursor = conn.cursor()
    try:
        cursor.execute(
            """UPDATE parish_intentions 
               SET author_parish_id = %s, content = %s, category = %s, is_anonymous = %s 
               WHERE id = %s""",
            (parish_id, content, category, is_anonymous, intention_id)
        )
        conn.commit()
        return {"status": "success"}
    except Exception as e:
        return JSONResponse(status_code=500, content={"status": "error", "message": str(e)})
    finally:
        cursor.close()
        conn.close()

@app.delete("/api/admin/intentions/delete/{intention_id}")
async def delete_intention(intention_id: int, token: str = Query(None)):
    if token != ADMIN_TOKEN: raise HTTPException(status_code=403)
    conn = mysql.connector.connect(**db_config)
    cursor = conn.cursor()
    try:
        cursor.execute("DELETE FROM parish_intentions WHERE id = %s", (intention_id,))
        conn.commit()
        return {"status": "success"}
    except Exception as e:
        return JSONResponse(status_code=500, content={"status": "error", "message": str(e)})
    finally:
        cursor.close()
        conn.close()
        
@app.post("/api/admin/candles/extinguish/{candle_id}")
async def admin_extinguish_candle(candle_id: int, token: str = Query(None)):
    if token != ADMIN_TOKEN: raise HTTPException(status_code=403)
    conn = None
    try:
        conn = mysql.connector.connect(**db_config)
        cursor = conn.cursor(dictionary=True)
        
        cursor.execute("""
            SELECT ic.id, COALESCE(pi.author_parish_id, up.home_parish_id) as target_parish_id, up_author.fcm_token
            FROM intention_candles ic
            JOIN parish_intentions pi ON ic.intention_id = pi.id
            LEFT JOIN user_profiles up ON pi.author_device_id = up.device_id
            LEFT JOIN user_profiles up_author ON pi.author_device_id = up_author.device_id
            WHERE ic.id = %s
        """, (candle_id,))
        
        candle_data = cursor.fetchone()
        if not candle_data:
            return JSONResponse(status_code=404, content={"error": "Świeca nie istnieje"})
            
        if candle_data['target_parish_id']:
            cursor.execute("""
                UPDATE parishes SET active_candles = GREATEST(0, active_candles - 1), last_update = NOW() 
                WHERE id = %s
            """, (candle_data['target_parish_id'],))
            
        cursor.execute("DELETE FROM intention_candles WHERE id = %s", (candle_id,))
        conn.commit()

        if candle_data.get('fcm_token'):
            asyncio.create_task(send_candle_notification_bg(candle_data['fcm_token'], -1))

        return {"status": "success"}
    except Exception as e:
        if conn: conn.rollback()
        return JSONResponse(status_code=500, content={"status": "error", "message": str(e)})
    finally:
        if conn and conn.is_connected():
            cursor.close()
            conn.close()

@app.post("/api/admin/candles/extend/{candle_id}")
async def admin_extend_candle(candle_id: int, token: str = Query(None)):
    if token != ADMIN_TOKEN: raise HTTPException(status_code=403)
    conn = None
    try:
        conn = mysql.connector.connect(**db_config)
        cursor = conn.cursor()
        
        cursor.execute("""
            UPDATE intention_candles 
            SET expires_at = IF(expires_at > NOW(), DATE_ADD(expires_at, INTERVAL 24 HOUR), DATE_ADD(NOW(), INTERVAL 24 HOUR))
            WHERE id = %s
        """, (candle_id,))
        
        conn.commit()
        return {"status": "success"}
    except Exception as e:
        if conn: conn.rollback()
        return JSONResponse(status_code=500, content={"status": "error", "message": str(e)})
    finally:
        if conn and conn.is_connected():
            cursor.close()
            conn.close()
            
@app.post("/api/admin/candles/create")
async def admin_light_candle(
    intention_id: int = Form(...),
    parish_id: str = Form(...),
    token: str = Query(None)
):
    if token != ADMIN_TOKEN: raise HTTPException(status_code=403)
    conn = None
    try:
        from datetime import datetime, timedelta
        conn = mysql.connector.connect(**db_config)
        cursor = conn.cursor(dictionary=True)

        cursor.execute("SELECT admin_device_id, admin_fcm_token FROM admin_id WHERE id = 1")
        admin_creds = cursor.fetchone()
        
        my_device_id = admin_creds['admin_device_id'] if admin_creds and admin_creds['admin_device_id'] else 'ADMIN_HUB'
        my_fcm_token = admin_creds['admin_fcm_token'] if admin_creds else None

        cursor.execute("SELECT name FROM parishes WHERE id = %s", (parish_id,))
        parish_data = cursor.fetchone()
        parish_name = parish_data['name'] if parish_data else "Twojej parafii"

        expires_at = datetime.now() + timedelta(hours=24)
        cursor.execute("""
            INSERT INTO intention_candles (intention_id, device_id, candle_type, expires_at)
            VALUES (%s, %s, 'candle', %s)
        """, (intention_id, my_device_id, expires_at))

        cursor.execute("""
            UPDATE parishes SET active_candles = active_candles + 1, last_update = NOW() 
            WHERE id = %s
        """, (parish_id,))
        
        conn.commit()

        if my_fcm_token:
            asyncio.create_task(send_candle_notification_bg(my_fcm_token, 24, parish_name))

        return {"status": "success"}
    except Exception as e:
        if conn: conn.rollback()
        return JSONResponse(status_code=500, content={"status": "error", "message": str(e)})
    finally:
        if conn and conn.is_connected():
            cursor.close()
            conn.close()
            
@app.post("/api/admin/intentions/pin/{intention_id}")
async def admin_toggle_pin(intention_id: int, token: str = Query(None)):
    if token != ADMIN_TOKEN: raise HTTPException(status_code=403)
    conn = None
    try:
        conn = mysql.connector.connect(**db_config)
        cursor = conn.cursor()
        cursor.execute("UPDATE parish_intentions SET is_pinned = 1 - is_pinned WHERE id = %s", (intention_id,))
        conn.commit()
        return {"status": "success"}
    except Exception as e:
        if conn: conn.rollback()
        return JSONResponse(status_code=500, content={"status": "error", "message": str(e)})
    finally:
        if conn and conn.is_connected():
            cursor.close()
            conn.close()

@app.post("/api/admin/intentions/pray/{intention_id}")
async def admin_toggle_prayer(intention_id: int, background_tasks: BackgroundTasks, token: str = Query(None)):
    if token != ADMIN_TOKEN: raise HTTPException(status_code=403)
    conn = None
    try:
        conn = mysql.connector.connect(**db_config)
        cursor = conn.cursor(dictionary=True)
        
        cursor.execute("SELECT admin_device_id FROM admin_id WHERE id = 1")
        admin_data = cursor.fetchone()
        admin_device_id = admin_data['admin_device_id'] if admin_data and admin_data['admin_device_id'] else 'ADMIN_HUB'
        
        cursor.execute("SELECT 1 FROM parish_intention_prayers WHERE intention_id = %s AND user_device_id = %s", (intention_id, admin_device_id))
        exists = cursor.fetchone()
        
        if exists:
            cursor.execute("DELETE FROM parish_intention_prayers WHERE intention_id = %s AND user_device_id = %s", (intention_id, admin_device_id))
            status = "removed"
        else:
            cursor.execute("SELECT home_parish_id FROM user_profiles WHERE device_id = %s", (admin_device_id,))
            user_profile = cursor.fetchone()
            current_parish_id = user_profile['home_parish_id'] if user_profile else None
            
            cursor.execute("INSERT INTO parish_intention_prayers (intention_id, user_device_id, praying_parish_id) VALUES (%s, %s, %s)", (intention_id, admin_device_id, current_parish_id))
            status = "added"

            cursor.execute("""
                SELECT up.fcm_token, pi.author_device_id 
                FROM parish_intentions pi 
                JOIN user_profiles up ON pi.author_device_id = up.device_id 
                WHERE pi.id = %s
            """, (intention_id,))
            author_data = cursor.fetchone()

            if author_data and author_data['fcm_token'] and author_data['author_device_id'] != admin_device_id:
                if current_parish_id:
                    cursor.execute("SELECT name FROM parishes WHERE id = %s", (current_parish_id,))
                    praying_parish = cursor.fetchone()
                    parish_name = praying_parish['name'] if praying_parish else "innej wspólnoty"
                else:
                    parish_name = "innej wspólnoty"
                    
                background_tasks.add_task(send_prayer_notification_bg, author_data['fcm_token'], parish_name)
                
        conn.commit()
        return {"status": "success", "action": status}
    except Exception as e:
        if conn: conn.rollback()
        return JSONResponse(status_code=500, content={"status": "error", "message": str(e)})
    finally:
        if conn and conn.is_connected():
            cursor.close()
            conn.close()

@app.get("/panel/events/list")
async def get_parish_events(
    parish_id: Optional[str] = Query(None),
    since: Optional[str] = Query(None)
):
    """Zwraca nadchodzące wydarzenia parafialne dla aplikacji mobilnej"""
    conn = None
    try:
        conn = mysql.connector.connect(**db_config)
        cursor = conn.cursor(dictionary=True)

        query = """
            SELECT id, parish_id as parishId, event_date as eventDate, title, description
            FROM parish_events
            WHERE event_date >= DATE_SUB(NOW(), INTERVAL 1 DAY)
        """
        params = []

        if parish_id:
            query += " AND parish_id = %s"
            params.append(parish_id)

        if since:
            query += " AND created_at > %s"
            params.append(since)

        query += " ORDER BY event_date ASC"

        cursor.execute(query, params)
        events = cursor.fetchall()

        for e in events:
            if isinstance(e['eventDate'], datetime):
                e['eventDate'] = e['eventDate'].strftime('%Y-%m-%d %H:%M:%S')

        return events
    except Exception as e:
        print(f"BŁĄD GET /panel/events/list: {e}")
        return []
    finally:
        if conn and conn.is_connected():
            cursor.close()
            conn.close()


@app.post("/panel/events/add")
async def create_parish_event(
    background_tasks: BackgroundTasks,
    parish_id: str = Form(...),
    event_date: str = Form(...),
    title: str = Form(...),
    description: Optional[str] = Form(None)
):
    """Endpoint do dodawania nowego wydarzenia z panelu WWW"""
    conn = None
    try:
        conn = mysql.connector.connect(**db_config)
        cursor = conn.cursor()

        formatted_date = event_date.replace("T", " ")
        if len(formatted_date) == 16:
            formatted_date += ":00"

        query = """
            INSERT INTO parish_events (parish_id, event_date, title, description)
            VALUES (%s, %s, %s, %s)
        """
        cursor.execute(query, (parish_id, formatted_date, title, description if description and description.strip() else None))
        
        new_event_id = cursor.lastrowid
        conn.commit()

        cursor.execute("UPDATE parishes SET last_update = NOW() WHERE id = %s", (parish_id,))
        conn.commit()

        cursor.execute("SELECT name FROM parishes WHERE id = %s", (parish_id,))
        p_data = cursor.fetchone()
        parish_name = p_data[0] if p_data else "Twojej parafii"
        
        background_tasks.add_task(send_event_notification, parish_id, parish_name, title)

        return {"status": "success", "event_id": new_event_id}
    except Exception as e:
        if conn: conn.rollback()
        print(f"BŁĄD POST /panel/events/add: {e}")
        return JSONResponse(status_code=500, content={"status": "error", "message": str(e)})
    finally:
        if conn and conn.is_connected():
            cursor.close()
            conn.close()


@app.post("/panel/events/edit/{event_id}")
async def edit_parish_event(
    event_id: int,
    event_date: str = Form(...),
    title: str = Form(...),
    description: Optional[str] = Form(None)
):
    """Endpoint do edycji istniejącego wydarzenia z panelu WWW"""
    conn = None
    try:
        conn = mysql.connector.connect(**db_config)
        cursor = conn.cursor()

        formatted_date = event_date.replace("T", " ")
        if len(formatted_date) == 16:
            formatted_date += ":00"

        query = """
            UPDATE parish_events 
            SET event_date = %s, title = %s, description = %s
            WHERE id = %s
        """
        cursor.execute(query, (formatted_date, title, description if description and description.strip() else None, event_id))
        conn.commit()

        return {"status": "success", "event_id": event_id}
    except Exception as e:
        if conn: conn.rollback()
        print(f"BŁĄD POST /panel/events/edit: {e}")
        return JSONResponse(status_code=500, content={"status": "error", "message": str(e)})
    finally:
        if conn and conn.is_connected():
            cursor.close()
            conn.close()


@app.post("/panel/events/delete/{event_id}")
async def delete_parish_event(event_id: int):
    """Endpoint do usuwania wydarzenia z panelu WWW"""
    conn = None
    try:
        conn = mysql.connector.connect(**db_config)
        cursor = conn.cursor()
        cursor.execute("DELETE FROM parish_events WHERE id = %s", (event_id,))
        conn.commit()
        return {"status": "success"}
    except Exception as e:
        if conn: conn.rollback()
        print(f"BŁĄD USUWANIA WYDARZENIA: {e}")
        return JSONResponse(status_code=500, content={"status": "error", "message": str(e)})
    finally:
        if conn and conn.is_connected():
            cursor.close()
            conn.close()
        

@app.get("/user/stats")
def get_user_stats(device_id: str = Query(...)):
    try:
        conn = mysql.connector.connect(**db_config)
        cursor = conn.cursor(dictionary=True)
        cursor.execute("SELECT points FROM user_profiles WHERE device_id = %s", (device_id,))
        result = cursor.fetchone()
        points = result['points'] if result and result['points'] is not None else 0
        cursor.close()
        conn.close()
        return {
            "points": points,
            "next_reward": 10 if points < 10 else (50 if points < 50 else 200),
            "has_crown": points >= 10,
            "has_premium_reward": points >= 50,
            "has_tshirt_reward": points >= 200
        }
    except Exception as e:
        return {"points": 0, "next_reward": 10, "has_crown": False, "has_premium_reward": False, "has_tshirt_reward": False}


async def send_candle_notification_bg(token: str, duration: int, parish_name: str = None):
    """Asynchroniczna funkcja do wysyłania powiadomienia o zapalonej/zgaszonej świecy"""
    
    if duration == -1:
        title_text = "Świeca wygasła 🕯️"
        body_text = "Twoja świeca w intencji wygasła. Możesz zapalić nową!"
    else:
        title_text = "Płomyk Nadziei! 🕯️"
        if parish_name:
            body_text = f"Zapalono świecę w Twojej intencji w: {parish_name}"
        else:
            body_text = "Ktoś zapalił świecę w Twojej intencji."
    
    print(f"[DEBUG] Próba wysyłki z Hubu na token: {token}", flush=True)
    
    try:
        message = messaging.Message(
            notification=messaging.Notification(title=title_text, body=body_text),
            data={
                "title": title_text,
                "body": body_text,
                "action": "open_intentions"
            },
            apns=messaging.APNSConfig(
                headers={"apns-priority": "10"},
                payload=messaging.APNSPayload(
                    aps=messaging.Aps(sound="default", badge=1)
                )
            ),
            token=token,
        )
        response = await asyncio.to_thread(messaging.send, message)
        print(f"Powiadomienie FCM o świecy wysłane pomyślnie. ID: {response}", flush=True)
        
    except Exception as e:
        # ... (zachowaj swoją obecną logikę czyszczenia błędnych tokenów z bazy)
        print(f"Błąd FCM: {e}", flush=True)
        
def send_event_notification(parish_id, parish_name, event_title):
    """Wysyła powiadomienie do osób śledzących parafię o nowym wydarzeniu"""
    title_text = "Nowe Wydarzenie! 📅"
    body_text = f"W parafii {parish_name} zaplanowano: {event_title}."
    
    try:
        topic = f"parish_{parish_id}"
        message = messaging.Message(
            notification=messaging.Notification(
                title=title_text,
                body=body_text
            ),
            data={
                "title": title_text,
                "body": body_text,
                "parish_id": str(parish_id),
                "action": "open_parish"
            },
            apns=messaging.APNSConfig(
                headers={
                    "apns-priority": "10",
                },
                payload=messaging.APNSPayload(
                    aps=messaging.Aps(
                        sound="default",
                        badge=1
                    )
                )
            ),
            topic=topic
        )
        messaging.send(message)
        print(f"DEBUG: Wysłano powiadomienie o wydarzeniu dla {parish_name}", flush=True)
    except Exception as e:
        print(f"Błąd FCM: {e}", flush=True)
        

@app.get("/api/admin/config")
async def get_admin_config():
    conn = None
    try:
        conn = mysql.connector.connect(**db_config)
        cursor = conn.cursor(dictionary=True)
        
        # Pobieramy dane z tabeli admin_id
        cursor.execute("SELECT admin_device_id, admin_fcm_token FROM admin_id WHERE id = 1")
        result = cursor.fetchone()
        
        if not result:
            return {"admin_device_id": "", "admin_fcm_token": ""}
            
        return result
    finally:
        if conn and conn.is_connected():
            cursor.close()
            conn.close()
            
@app.post("/api/admin/update-device-id")
async def update_admin_device_id(
    device_id: str = Form(...),
    token: str = Query(None)
):
    if token != ADMIN_TOKEN: raise HTTPException(status_code=403)
    conn = None
    try:
        conn = mysql.connector.connect(**db_config)
        cursor = conn.cursor()
        
        cursor.execute("""
            UPDATE admin_id 
            SET admin_device_id = %s, admin_fcm_token = '' 
            WHERE id = 1
        """, (device_id,))
        
        conn.commit()
        return {"status": "success"}
    except Exception as e:
        if conn: conn.rollback()
        return JSONResponse(status_code=500, content={"status": "error", "message": str(e)})
    finally:
        if conn and conn.is_connected():
            cursor.close()
            conn.close()
            

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8004)

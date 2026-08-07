#!/bin/bash
echo "--- Zamykanie wiszących procesów Javy i Kotlina ---"
./gradlew --stop
pkill -f kotlin
pkill -f java

echo "--- Czyszczenie pamięci podręcznej Xcode (DerivedData) ---"
rm -rf ~/Library/Developer/Xcode/DerivedData

echo "--- Czyszczenie pamięci kompilatora Kotlin/Native (Konan) ---"
rm -rf ~/.konan/cache

echo "--- Twarde czyszczenie katalogów budowania projektu KMP ---"
rm -rf .gradle
rm -rf .kotlin
rm -rf build
rm -rf iosApp/build

echo "--- Wywoływanie czystego startu Gradle ---"
./gradlew clean

echo "✅ Środowisko zresetowane! Możesz teraz uruchomić Debug w Android Studio."

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;


CREATE TABLE `admin_fcm_token` (
  `id` int(11) NOT NULL,
  `token` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `admin_id` (
  `id` int(11) NOT NULL DEFAULT 1,
  `admin_fcm_token` varchar(255) DEFAULT NULL,
  `admin_device_id` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `favorite_logs` (
  `id` int(11) NOT NULL,
  `parish_id` varchar(50) NOT NULL,
  `parish_name` varchar(255) NOT NULL,
  `action` enum('added','removed') NOT NULL,
  `created_at` timestamp NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `intention_candles` (
  `id` int(11) NOT NULL,
  `intention_id` int(11) NOT NULL,
  `device_id` varchar(255) NOT NULL,
  `candle_type` varchar(50) NOT NULL DEFAULT 'candle',
  `created_at` timestamp NULL DEFAULT current_timestamp(),
  `expires_at` timestamp NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `news_feed` (
  `id` int(11) NOT NULL,
  `title` varchar(255) NOT NULL,
  `content` text NOT NULL,
  `image_url` varchar(512) DEFAULT NULL,
  `action_link` varchar(512) DEFAULT NULL,
  `publish_date` datetime DEFAULT current_timestamp(),
  `is_visible` tinyint(1) DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `new_parish` (
  `id` int(11) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `address` varchar(255) DEFAULT NULL,
  `latitude` decimal(10,8) DEFAULT NULL,
  `longitude` decimal(11,8) DEFAULT NULL,
  `massHoursSunday` text DEFAULT NULL,
  `hasMassSundayHour` text DEFAULT NULL,
  `hasMassForChildrenHour` text DEFAULT NULL,
  `massHoursMonday` text DEFAULT NULL,
  `confessionInfo` text DEFAULT NULL,
  `adorationInfo` text DEFAULT NULL,
  `officeHoursText` text DEFAULT NULL,
  `phoneNum` varchar(50) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `websiteUrl` varchar(255) DEFAULT NULL,
  `bankAccountNumber` varchar(100) DEFAULT NULL,
  `blikNumber` varchar(255) DEFAULT NULL,
  `pastorName` varchar(255) DEFAULT NULL,
  `diocese` varchar(255) DEFAULT NULL,
  `deanery` varchar(255) DEFAULT NULL,
  `foundingYear` varchar(20) DEFAULT NULL,
  `socialMediaFacebook` varchar(255) DEFAULT NULL,
  `socialMediaYouTube` varchar(255) DEFAULT NULL,
  `socialMediaInstagram` varchar(255) DEFAULT NULL,
  `firstSaturdayOfMonth_hour` varchar(20) DEFAULT NULL,
  `firstSaturdayOfMonth_info` text DEFAULT NULL,
  `status` varchar(50) DEFAULT 'pending',
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `photoUrl` text DEFAULT NULL,
  `massHoursTuesday` text DEFAULT NULL,
  `massHoursWednesday` text DEFAULT NULL,
  `massHoursThursday` text DEFAULT NULL,
  `massHoursFriday` text DEFAULT NULL,
  `massHoursSaturday` text DEFAULT NULL,
  `author_device_id` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `parishes` (
  `id` varchar(50) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `first_subscription_date` datetime DEFAULT NULL,
  `last_subscription_date` datetime DEFAULT NULL,
  `address` varchar(255) DEFAULT NULL,
  `diocese` varchar(255) DEFAULT NULL,
  `deanery` varchar(255) DEFAULT NULL,
  `pastorName` varchar(255) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `websiteUrl` varchar(255) DEFAULT NULL,
  `phoneNum` varchar(255) DEFAULT NULL,
  `latitude` double DEFAULT NULL,
  `longitude` double DEFAULT NULL,
  `foundingYear` varchar(50) DEFAULT NULL,
  `hasMassSunday` tinyint(1) DEFAULT NULL,
  `hasMassSundayHour` varchar(100) DEFAULT NULL,
  `adorationInfo` text DEFAULT NULL,
  `hasMassForChildren` tinyint(1) DEFAULT NULL,
  `hasMassForChildrenHour` varchar(100) DEFAULT NULL,
  `massHoursMonday` text DEFAULT NULL,
  `massHoursTuesday` text DEFAULT NULL,
  `massHoursWednesday` text DEFAULT NULL,
  `massHoursThursday` text DEFAULT NULL,
  `massHoursFriday` text DEFAULT NULL,
  `massHoursSaturday` text DEFAULT NULL,
  `massHoursSunday` text DEFAULT NULL,
  `officeHoursText` text DEFAULT NULL,
  `announcements` text DEFAULT NULL,
  `intentions` text DEFAULT NULL,
  `confessionInfo` text DEFAULT NULL,
  `donationInfo` text DEFAULT NULL,
  `bankAccountNumber` varchar(255) DEFAULT NULL,
  `blikNumber` varchar(255) DEFAULT NULL,
  `photoUrl` text DEFAULT NULL,
  `socialMediaFacebook` text DEFAULT NULL,
  `socialMediaYouTube` text DEFAULT NULL,
  `socialMediaInstagram` text DEFAULT NULL,
  `last_update` timestamp NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `isCathedral` tinyint(1) DEFAULT NULL,
  `access_token` varchar(100) DEFAULT NULL,
  `pin_code` varchar(4) DEFAULT '1234',
  `subscription_expires` datetime DEFAULT NULL,
  `is_active` tinyint(1) DEFAULT 1,
  `firstSaturdayOfMonth` tinyint(1) NOT NULL DEFAULT 0,
  `firstSaturdayOfMonth_hour` varchar(255) DEFAULT '',
  `firstSaturdayOfMonth_info` text DEFAULT NULL,
  `active_intentions` int(11) NOT NULL DEFAULT 0,
  `active_candles` int(11) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `parish_events` (
  `id` bigint(20) NOT NULL,
  `parish_id` varchar(255) NOT NULL,
  `event_date` datetime NOT NULL,
  `title` varchar(255) NOT NULL,
  `description` text DEFAULT NULL,
  `created_at` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `parish_intentions` (
  `id` int(11) NOT NULL,
  `content` text NOT NULL,
  `author_device_id` varchar(255) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT current_timestamp(),
  `is_active` tinyint(1) DEFAULT 1,
  `device_id` varchar(50) DEFAULT NULL,
  `category` varchar(50) DEFAULT 'Ogólna',
  `is_anonymous` tinyint(1) DEFAULT 0,
  `country` varchar(2) DEFAULT 'PL',
  `author_parish_id` varchar(255) DEFAULT NULL,
  `is_pinned` tinyint(1) DEFAULT 0,
  `expires_at` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `parish_intention_prayers` (
  `intention_id` int(11) NOT NULL,
  `user_device_id` varchar(255) NOT NULL,
  `praying_parish_id` varchar(50) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `priest_collaborations` (
  `id` int(11) NOT NULL,
  `parish_id` varchar(255) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `proposals` (
  `id` int(11) NOT NULL,
  `parish_id` varchar(50) DEFAULT NULL,
  `massHoursSunday` text DEFAULT NULL,
  `hasMassSundayHour` text DEFAULT NULL,
  `hasMassForChildrenHour` text DEFAULT NULL,
  `massHoursMonday` text DEFAULT NULL,
  `massHoursTuesday` text DEFAULT NULL,
  `massHoursWednesday` text DEFAULT NULL,
  `massHoursThursday` text DEFAULT NULL,
  `massHoursFriday` text DEFAULT NULL,
  `massHoursSaturday` text DEFAULT NULL,
  `confessionInfo` text DEFAULT NULL,
  `adorationInfo` text DEFAULT NULL,
  `officeHoursText` text DEFAULT NULL,
  `phoneNum` varchar(50) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `websiteUrl` varchar(255) DEFAULT NULL,
  `photoUrl` text DEFAULT NULL,
  `bankAccountNumber` varchar(100) DEFAULT NULL,
  `blikNumber` varchar(255) DEFAULT NULL,
  `pastorName` varchar(255) DEFAULT NULL,
  `diocese` varchar(255) DEFAULT NULL,
  `deanery` varchar(255) DEFAULT NULL,
  `foundingYear` varchar(20) DEFAULT NULL,
  `socialMediaFacebook` varchar(255) DEFAULT NULL,
  `socialMediaYouTube` varchar(255) DEFAULT NULL,
  `socialMediaInstagram` varchar(255) DEFAULT NULL,
  `status` varchar(50) DEFAULT 'pending',
  `created_at` timestamp NULL DEFAULT current_timestamp(),
  `latitude` decimal(10,8) DEFAULT NULL,
  `longitude` decimal(11,8) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `address` varchar(255) DEFAULT NULL,
  `firstSaturdayOfMonth` tinyint(1) NOT NULL DEFAULT 0,
  `firstSaturdayOfMonth_hour` varchar(20) DEFAULT NULL,
  `firstSaturdayOfMonth_info` text DEFAULT NULL,
  `announcements` text DEFAULT NULL,
  `donationInfo` text DEFAULT NULL,
  `isCathedral` tinyint(1) DEFAULT 0,
  `last_update` timestamp NULL DEFAULT current_timestamp(),
  `is_active` tinyint(1) DEFAULT 1,
  `author_device_id` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `server_stats` (
  `id` int(11) NOT NULL,
  `download_count` int(11) DEFAULT 0,
  `html_visitors` int(11) DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `tshirt_orders` (
  `id` int(11) NOT NULL,
  `device_id` varchar(255) NOT NULL,
  `name` varchar(255) NOT NULL,
  `address` text NOT NULL,
  `size` varchar(10) NOT NULL,
  `phone` varchar(50) NOT NULL,
  `created_at` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `user_candle_inventory` (
  `device_id` varchar(255) NOT NULL,
  `candle_8h` int(11) DEFAULT 0,
  `candle_12h` int(11) DEFAULT 0,
  `candle_24h` int(11) DEFAULT 0,
  `updated_at` timestamp NULL DEFAULT current_timestamp() ON UPDATE current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `user_profiles` (
  `device_id` varchar(255) NOT NULL,
  `home_parish_id` varchar(50) DEFAULT NULL,
  `fcm_token` varchar(255) DEFAULT NULL,
  `points` int(11) DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;


ALTER TABLE `admin_fcm_token`
  ADD PRIMARY KEY (`id`);

ALTER TABLE `admin_id`
  ADD PRIMARY KEY (`id`);

ALTER TABLE `favorite_logs`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_parish_id` (`parish_id`);

ALTER TABLE `intention_candles`
  ADD PRIMARY KEY (`id`),
  ADD KEY `intention_id` (`intention_id`);

ALTER TABLE `news_feed`
  ADD PRIMARY KEY (`id`);

ALTER TABLE `new_parish`
  ADD PRIMARY KEY (`id`);

ALTER TABLE `parishes`
  ADD PRIMARY KEY (`id`);

ALTER TABLE `parish_events`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_parish_id` (`parish_id`);

ALTER TABLE `parish_intentions`
  ADD PRIMARY KEY (`id`);

ALTER TABLE `parish_intention_prayers`
  ADD PRIMARY KEY (`intention_id`,`user_device_id`);

ALTER TABLE `priest_collaborations`
  ADD PRIMARY KEY (`id`);

ALTER TABLE `proposals`
  ADD PRIMARY KEY (`id`);

ALTER TABLE `server_stats`
  ADD PRIMARY KEY (`id`);

ALTER TABLE `tshirt_orders`
  ADD PRIMARY KEY (`id`);

ALTER TABLE `user_candle_inventory`
  ADD PRIMARY KEY (`device_id`);

ALTER TABLE `user_profiles`
  ADD PRIMARY KEY (`device_id`),
  ADD KEY `fk_home_parish` (`home_parish_id`);


ALTER TABLE `favorite_logs`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

ALTER TABLE `intention_candles`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

ALTER TABLE `news_feed`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

ALTER TABLE `new_parish`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

ALTER TABLE `parish_events`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

ALTER TABLE `parish_intentions`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

ALTER TABLE `priest_collaborations`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

ALTER TABLE `proposals`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

ALTER TABLE `tshirt_orders`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;


ALTER TABLE `intention_candles`
  ADD CONSTRAINT `intention_candles_ibfk_1` FOREIGN KEY (`intention_id`) REFERENCES `parish_intentions` (`id`) ON DELETE CASCADE;

ALTER TABLE `parish_intention_prayers`
  ADD CONSTRAINT `parish_intention_prayers_ibfk_1` FOREIGN KEY (`intention_id`) REFERENCES `parish_intentions` (`id`) ON DELETE CASCADE;

ALTER TABLE `user_profiles`
  ADD CONSTRAINT `fk_home_parish` FOREIGN KEY (`home_parish_id`) REFERENCES `parishes` (`id`) ON DELETE SET NULL;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;

-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: Jan 06, 2026 at 06:09 PM
-- Server version: 11.8.3-MariaDB
-- PHP Version: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `cyberaware_db`
--

-- --------------------------------------------------------

--
-- Table structure for table `reports`
--

CREATE TABLE `reports` (
  `id` bigint(20) NOT NULL,
  `ai_result` varchar(255) DEFAULT NULL,
  `message` varchar(255) DEFAULT NULL,
  `sender` varchar(255) DEFAULT NULL,
  `timestamp` datetime(6) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `reports`
--

INSERT INTO `reports` (`id`, `ai_result`, `message`, `sender`, `timestamp`) VALUES
(4, 'Verified Scam ✅', 'ṚocḲet-এ ৩,০০০ TK 1855666069 এ/c-তে এসেছে। ডিটেইলস cutt.ly/stwVNmIC', '01869712645', '2026-01-06 04:49:14.675905'),
(5, 'AI Analysis Pending', 'ṚocḲet-এ ৩,০০০ TK 1855666069 এ/c-তে এসেছে। ডিটেইলস cutt.ly/stwVNmIC', '01856982132', '2026-01-06 05:09:27.497413'),
(6, 'Verified Scam ✅', 'ṚocḲet-এ ৩,০০০ TK 1855666069 এ/c-তে এসেছে। ডিটেইলস cutt.ly/stwVNmIC', '01856982132', '2026-01-06 05:10:33.347954'),
(7, 'Verified Scam ✅', 'ṚocḲet-এ ৩,০০০ TK 1855666069 এ/c-তে এসেছে। ডিটেইলস cutt.ly/stwVNmIC', '01856982132', '2026-01-06 05:17:22.113214'),
(8, 'Verified Scam ✅', 'ṚocḲet-এ ৩,০০০ TK 1855666069 এ/c-তে এসেছে। ডিটেইলস cutt.ly/stwVNmIC', '01856982132', '2026-01-06 05:17:42.616903'),
(9, 'AI Analysis Pending', 'ṚocḲet-এ ৩,০০০ TK 1855666069 এ/c-তে এসেছে। ডিটেইলস cutt.ly/stwVNmIC', '01856982132', '2026-01-06 05:18:03.492104'),
(10, 'AI Analysis Pending', 'ṚocḲet-এ ৩,০০০ TK 1855666069 এ/c-তে এসেছে। ডিটেইলস cutt.ly/stwVNmIC', '0147586522', '2026-01-06 05:20:28.185895'),
(11, 'Verified Scam ✅', 'ṚocḲet-এ ৩,০০০ TK 1855666069 এ/c-তে এসেছে। ডিটেইলস cutt.ly/stwVNmIC', '0147586522', '2026-01-06 05:20:40.696752'),
(12, 'AI Analysis Pending', '৫৫৫ - বিডিটি, আপনি কি মিস করবেন? cutt.ly/orCmcTIC?লেজনম্বর069', '015645213879', '2026-01-06 19:24:27.752025'),
(13, 'Verified Scam ✅', '৫৫৫ - বিডিটি, আপনি কি মিস করবেন? cutt.ly/orCmcTIC?লেজনম্বর069', '+8801805120201', '2026-01-06 19:31:35.387306'),
(14, 'Pending Analysis', '৫৫৫ - বিডিটি, আপনি কি মিস করবেন? cutt.ly/orCmcTIC?লেজনম্বর069', '+8801805120201', NULL),
(15, 'Pending Analysis', 'খেলা https://cutt.ly/ftwW0MUd', '+8801911267084', NULL);

--
-- Indexes for dumped tables
--

--
-- Indexes for table `reports`
--
ALTER TABLE `reports`
  ADD PRIMARY KEY (`id`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `reports`
--
ALTER TABLE `reports`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=16;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;

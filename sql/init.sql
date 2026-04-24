-- MySQL dump 10.13  Distrib 8.0.40, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: streaming_db
-- ------------------------------------------------------
-- Server version	8.0.40

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `device`
--

DROP TABLE IF EXISTS `device`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `device` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '设备ID',
  `name` varchar(100) NOT NULL COMMENT '设备名称',
  `brand` varchar(50) DEFAULT NULL,
  `ip` varchar(45) NOT NULL COMMENT '摄像头IP',
  `username` varchar(50) NOT NULL COMMENT 'RTSP用户名',
  `password` varchar(255) NOT NULL COMMENT '加密后的密码',
  `channel` int DEFAULT '101' COMMENT '通道号',
  `rtsp_url` varchar(500) DEFAULT NULL COMMENT '完整RTSP地址',
  `stream_id` varchar(100) DEFAULT NULL COMMENT 'ZLMediaKit流ID',
  `status` tinyint DEFAULT '0' COMMENT '0-离线 1-在线',
  `last_online_time` datetime DEFAULT NULL COMMENT '最后在线时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `last_seen` datetime DEFAULT NULL,
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间（NULL表示未删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `stream_id` (`stream_id`),
  KEY `idx_status` (`status`),
  KEY `idx_stream_id` (`stream_id`),
  KEY `idx_deleted_at` (`deleted_at`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='摄像头设备表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `device`
--

LOCK TABLES `device` WRITE;
/*!40000 ALTER TABLE `device` DISABLE KEYS */;
INSERT INTO `device` VALUES (1,'海康摄像头_主出入口','海康威视','192.168.1.64','admin','TDKb2101357',101,NULL,'camera_192_168_1_64',1,'2026-04-17 20:37:36','2026-04-13 16:58:20','2026-04-18 21:14:27',NULL,NULL),(3,'测试摄像头','其他','127.0.0.1','','BQGHoM3lqYcsurCRq3PlUw==',1,NULL,'camera_127_0_0_1',0,'2026-04-17 20:37:36','2026-04-13 20:52:15','2026-04-17 20:37:36',NULL,NULL),(11,'车库','海康威视','192.168.1.1','admin','',101,NULL,NULL,0,NULL,'2026-04-18 20:31:19','2026-04-18 20:31:32',NULL,'2026-04-18 20:31:32'),(12,'车库','海康威视','1.1.1.1','admin','',101,NULL,NULL,0,NULL,'2026-04-18 20:35:01','2026-04-18 20:35:05',NULL,'2026-04-18 20:35:05'),(13,'西大门','海康威视','192.168.118.186','admin','',101,NULL,'camera_192_168_118_186',1,NULL,'2026-04-18 20:35:35','2026-04-22 19:20:31',NULL,'2026-04-22 19:20:31');
/*!40000 ALTER TABLE `device` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `device_operation_history`
--

DROP TABLE IF EXISTS `device_operation_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `device_operation_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `device_id` int NOT NULL COMMENT '设备ID',
  `device_name` varchar(100) DEFAULT NULL COMMENT '设备名称（快照）',
  `device_ip` varchar(50) DEFAULT NULL COMMENT '设备IP（快照）',
  `operation_type` varchar(50) NOT NULL COMMENT '操作类型: ADD/EDIT/DELETE/RESTORE/PERMANENT_DELETE',
  `operation_desc` varchar(200) DEFAULT NULL COMMENT '操作描述',
  `operator_id` int DEFAULT '1' COMMENT '操作人ID',
  `operator_name` varchar(50) DEFAULT 'admin' COMMENT '操作人名称',
  `detail_before` text COMMENT '操作前数据快照（JSON）',
  `detail_after` text COMMENT '操作后数据快照（JSON）',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_device_id` (`device_id`),
  KEY `idx_operation_type` (`operation_type`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设备操作历史记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `device_operation_history`
--

LOCK TABLES `device_operation_history` WRITE;
/*!40000 ALTER TABLE `device_operation_history` DISABLE KEYS */;
INSERT INTO `device_operation_history` VALUES (1,10,'车库','1.1.1.1','DELETE','删除设备: 车库',1,'admin','{\"id\":10,\"name\":\"车库\",\"brand\":\"海康威视\",\"ip\":\"1.1.1.1\",\"username\":\"admin\",\"password\":\"\",\"channel\":101,\"status\":0,\"create_time\":\"2026-04-18 20:18:00\",\"update_time\":\"2026-04-18 20:18:00\"}',NULL,'2026-04-18 20:18:09'),(2,11,'车库','192.168.1.1','DELETE','删除设备: 车库',1,'admin','{\"id\":11,\"name\":\"车库\",\"brand\":\"海康威视\",\"ip\":\"192.168.1.1\",\"username\":\"admin\",\"password\":\"\",\"channel\":101,\"status\":0,\"create_time\":\"2026-04-18 20:31:19\",\"update_time\":\"2026-04-18 20:31:19\"}',NULL,'2026-04-18 20:31:32'),(3,12,'车库','1.1.1.1','DELETE','删除设备: 车库',1,'admin','{\"id\":12,\"name\":\"车库\",\"brand\":\"海康威视\",\"ip\":\"1.1.1.1\",\"username\":\"admin\",\"password\":\"\",\"channel\":101,\"status\":0,\"create_time\":\"2026-04-18 20:35:01\",\"update_time\":\"2026-04-18 20:35:01\"}',NULL,'2026-04-18 20:35:05'),(4,13,'西大门','192.168.118.186','DELETE','删除设备: 西大门',1,'admin','{\"id\":13,\"name\":\"西大门\",\"brand\":\"海康威视\",\"ip\":\"192.168.118.186\",\"username\":\"admin\",\"password\":\"\",\"channel\":101,\"status\":0,\"create_time\":\"2026-04-18 20:35:35\",\"update_time\":\"2026-04-18 20:35:35\"}',NULL,'2026-04-18 20:35:40'),(5,13,'西大门','192.168.118.186','RESTORE','恢复设备: 西大门',1,'admin','{\"id\":13,\"name\":\"西大门\",\"brand\":\"海康威视\",\"ip\":\"192.168.118.186\",\"username\":\"admin\",\"password\":\"\",\"channel\":101,\"status\":0,\"create_time\":\"2026-04-18 20:35:35\",\"update_time\":\"2026-04-18 20:35:39\",\"deleted_at\":\"2026-04-18 20:35:39\"}',NULL,'2026-04-18 20:35:43'),(6,1,'海康摄像头_主出入口','192.168.1.64','EDIT','编辑设备: 海康摄像头_主出入口',1,'admin','{\"id\":1,\"name\":\"海康摄像头_主出入口\",\"brand\":\"海康威视\",\"ip\":\"192.168.1.64\",\"username\":\"admin\",\"password\":\"\",\"channel\":101,\"stream_id\":\"camera_192_168_1_64\",\"status\":1,\"last_online_time\":\"2026-04-17 20:37:36\",\"create_time\":\"2026-04-13 16:58:20\",\"update_time\":\"2026-04-17 20:37:36\"}','{\"id\":1,\"name\":\"海康摄像头_主出入口\",\"ip\":\"192.168.1.64\",\"brand\":\"海康威视\",\"username\":\"admin\",\"password\":\"TDKb2101357\",\"channel\":101,\"status\":1,\"last_online_time\":\"2026-04-17T20:37:36\"}','2026-04-18 21:14:27'),(7,1,'海康摄像头_主出入口','192.168.1.64','EDIT','编辑设备: 海康摄像头_主出入口',1,'admin','{\"id\":1,\"name\":\"海康摄像头_主出入口\",\"brand\":\"海康威视\",\"ip\":\"192.168.1.64\",\"username\":\"admin\",\"password\":\"TDKb2101357\",\"channel\":101,\"stream_id\":\"camera_192_168_1_64\",\"status\":1,\"last_online_time\":\"2026-04-17 20:37:36\",\"create_time\":\"2026-04-13 16:58:20\",\"update_time\":\"2026-04-18 21:14:27\"}','{\"id\":1,\"name\":\"海康摄像头_主出入口\",\"ip\":\"192.168.1.64\",\"brand\":\"海康威视\",\"username\":\"admin\",\"password\":\"TDKb2101357\",\"channel\":101,\"status\":1,\"last_online_time\":\"2026-04-17T20:37:36\"}','2026-04-18 21:14:33'),(8,13,'西大门','192.168.118.186','EDIT','编辑设备: 西大门',1,'admin','{\"id\":13,\"name\":\"西大门\",\"brand\":\"海康威视\",\"ip\":\"192.168.118.186\",\"username\":\"admin\",\"password\":\"\",\"channel\":101,\"stream_id\":\"camera_192_168_118_186\",\"status\":1,\"create_time\":\"2026-04-18 20:35:35\",\"update_time\":\"2026-04-18 20:35:43\"}','{\"id\":13,\"name\":\"西大门\",\"ip\":\"192.168.118.186\",\"brand\":\"海康威视\",\"username\":\"admin\",\"password\":\"\",\"channel\":101,\"status\":1}','2026-04-20 19:35:42'),(9,13,'西大门','192.168.118.186','DELETE','删除设备: 西大门',1,'admin','{\"id\":13,\"name\":\"西大门\",\"brand\":\"海康威视\",\"ip\":\"192.168.118.186\",\"username\":\"admin\",\"password\":\"\",\"channel\":101,\"stream_id\":\"camera_192_168_118_186\",\"status\":1,\"create_time\":\"2026-04-18 20:35:35\",\"update_time\":\"2026-04-18 20:35:43\"}',NULL,'2026-04-22 19:20:32'),(10,1,'海康摄像头_主出入口','192.168.1.64','EDIT','编辑设备: 海康摄像头_主出入口',1,'admin','{\"id\":1,\"name\":\"海康摄像头_主出入口\",\"brand\":\"海康威视\",\"ip\":\"192.168.1.64\",\"username\":\"admin\",\"password\":\"TDKb2101357\",\"channel\":101,\"stream_id\":\"camera_192_168_1_64\",\"status\":0,\"last_online_time\":\"2026-04-17 20:37:36\",\"create_time\":\"2026-04-13 16:58:20\",\"update_time\":\"2026-04-18 21:14:27\"}','{\"id\":1,\"name\":\"海康摄像头_主出入口\",\"ip\":\"192.168.1.64\",\"brand\":\"海康威视\",\"username\":\"admin\",\"password\":\"TDKb2101357\",\"channel\":101,\"status\":0,\"last_online_time\":\"2026-04-17T20:37:36\"}','2026-04-22 19:34:38');
/*!40000 ALTER TABLE `device_operation_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `recording`
--

DROP TABLE IF EXISTS `recording`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `recording` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `device_id` bigint NOT NULL COMMENT '设备ID',
  `stream_id` varchar(100) NOT NULL COMMENT '流ID',
  `file_path` varchar(500) NOT NULL COMMENT '录像文件路径',
  `file_size` bigint DEFAULT '0' COMMENT '文件大小(字节)',
  `duration` int DEFAULT '0' COMMENT '时长(秒)',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `status` tinyint DEFAULT '0' COMMENT '0-录制中 1-已完成',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_device_id` (`device_id`),
  KEY `idx_start_time` (`start_time`),
  CONSTRAINT `recording_ibfk_1` FOREIGN KEY (`device_id`) REFERENCES `device` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='录像记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `recording`
--

LOCK TABLES `recording` WRITE;
/*!40000 ALTER TABLE `recording` DISABLE KEYS */;
/*!40000 ALTER TABLE `recording` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `recording_history`
--

DROP TABLE IF EXISTS `recording_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `recording_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `recording_id` bigint NOT NULL,
  `device_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `action` varchar(50) NOT NULL,
  `status` varchar(50) NOT NULL,
  `start_time` datetime DEFAULT NULL,
  `end_time` datetime DEFAULT NULL,
  `duration_seconds` int DEFAULT '0',
  `error_message` text,
  `file_size` bigint DEFAULT '0',
  `file_path` varchar(500) DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `record_type` varchar(20) DEFAULT 'manual' COMMENT '录制类型: auto(全天), manual(手动)',
  PRIMARY KEY (`id`),
  KEY `idx_recording_id` (`recording_id`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_record_type` (`record_type`)
) ENGINE=InnoDB AUTO_INCREMENT=31 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `recording_history`
--

LOCK TABLES `recording_history` WRITE;
/*!40000 ALTER TABLE `recording_history` DISABLE KEYS */;
INSERT INTO `recording_history` VALUES (1,1776773260000,1,1,'stop','deleted','2026-04-20 21:02:00','2026-04-20 21:03:00',60,NULL,0,'/opt/media/bin/www/record/live/camera_192_168_1_64/2026-04-20/2026-04-20-21-02-21-0.mp4','2026-04-21 20:07:40','manual'),(2,1776773260001,1,1,'stop','deleted','2026-04-20 21:10:00','2026-04-20 21:11:00',60,NULL,0,'/opt/media/bin/www/record/live/camera_192_168_1_64/2026-04-20/2026-04-20-21-10-11-0.mp4','2026-04-21 20:07:40','manual'),(3,1776773260002,1,1,'stop','deleted','2026-04-21 19:46:00','2026-04-21 19:47:00',60,NULL,0,'/opt/media/bin/www/record/live/camera_192_168_1_64/2026-04-21/.2026-04-21-19-46-21-0.mp4','2026-04-21 20:07:40','manual'),(4,1776773322294,1,1,'auto_start','failed','2026-04-21 20:08:42','2026-04-21 20:22:03',0,'被新录制覆盖',0,NULL,'2026-04-21 20:08:42','auto'),(5,1776774105897,1,1,'start','failed','2026-04-21 20:22:04','2026-04-21 20:24:23',0,'被新录制覆盖',0,NULL,'2026-04-21 20:22:04','manual'),(6,1776774105898,1,1,'start','failed','2026-04-21 20:24:23','2026-04-21 20:24:29',0,'被新录制覆盖',0,NULL,'2026-04-21 20:24:23','manual'),(7,1776774105899,1,1,'start','failed','2026-04-21 20:24:30','2026-04-21 21:22:43',0,'被新录制覆盖',0,NULL,'2026-04-21 20:24:30','manual'),(8,1776777727189,1,1,'start','failed','2026-04-21 21:22:44','2026-04-21 21:30:07',0,'被新录制覆盖',0,NULL,'2026-04-21 21:22:44','manual'),(9,1776778159011,1,1,'start','failed','2026-04-21 21:30:07','2026-04-21 21:37:57',0,'被新录制覆盖',0,NULL,'2026-04-21 21:30:07','manual'),(10,1776778666148,1,1,'start','failed','2026-04-21 21:37:57','2026-04-21 21:44:30',0,'被新录制覆盖',0,NULL,'2026-04-21 21:37:57','manual'),(11,1776779032478,1,1,'start','failed','2026-04-21 21:44:30','2026-04-21 21:49:18',0,'被新录制覆盖',0,NULL,'2026-04-21 21:44:30','manual'),(12,1776779345170,1,1,'start','failed','2026-04-21 21:49:18','2026-04-21 21:57:43',0,'被新录制覆盖',0,NULL,'2026-04-21 21:49:18','manual'),(13,1776779373031,1,1,'stop','success','2026-04-21 21:49:17','2026-04-21 21:49:33',16,NULL,8461105,'/opt/media/bin/www/record/live/camera_192_168_1_64/2026-04-21/2026-04-21-21-49-16-0.mp4','2026-04-21 21:49:33','auto'),(14,1776779831283,1,1,'start','failed','2026-04-21 21:57:44','2026-04-21 21:58:32',0,'被新录制覆盖',0,NULL,'2026-04-21 21:57:44','manual'),(15,1776779886846,1,1,'stop','success','2026-04-21 21:57:42','2026-04-21 21:58:06',24,NULL,11979139,'/opt/media/bin/www/record/live/camera_192_168_1_64/2026-04-21/2026-04-21-21-57-42-0.mp4','2026-04-21 21:58:06','manual'),(16,1776779831284,1,1,'start','failed','2026-04-21 21:58:33','2026-04-22 19:34:42',0,'被新录制覆盖',0,NULL,'2026-04-21 21:58:33','manual'),(17,1776779925689,1,1,'stop','success','2026-04-21 21:58:31','2026-04-21 21:58:45',14,NULL,7515853,'/opt/media/bin/www/record/live/camera_192_168_1_64/2026-04-21/2026-04-21-21-58-31-0.mp4','2026-04-21 21:58:45','manual'),(18,1776857654448,1,1,'start','failed','2026-04-22 19:34:42','2026-04-22 19:47:04',0,'被新录制覆盖',0,NULL,'2026-04-22 19:34:42','manual'),(19,1776857654449,1,1,'start','failed','2026-04-22 19:47:05','2026-04-22 19:48:40',0,'被新录制覆盖',0,NULL,'2026-04-22 19:47:05','manual'),(20,1776858451684,1,1,'stop','success','2026-04-22 19:46:56','2026-04-22 19:47:31',35,NULL,17679838,'/opt/media/bin/www/record/live/camera_192_168_1_64/2026-04-22/2026-04-22-19-46-58-0.mp4','2026-04-22 19:47:31','manual'),(21,1776857654450,1,1,'start','running','2026-04-22 19:48:41',NULL,0,NULL,0,NULL,'2026-04-22 19:48:41','manual'),(22,1776858535552,1,1,'stop','success','2026-04-22 19:48:08','2026-04-22 19:48:55',47,NULL,24101150,'/opt/media/bin/www/record/live/camera_192_168_1_64/2026-04-22/2026-04-22-19-48-09-0.mp4','2026-04-22 19:48:55','manual'),(23,1776862938821,1,1,'stop','deleted','2026-04-22 20:02:17','2026-04-22 21:02:18',3601,NULL,1820000606,'/opt/media/bin/www/record/live/camera_192_168_1_64/2026-04-22/2026-04-22-20-02-16-0.mp4','2026-04-22 21:02:18','manual'),(24,1776999415976,1,1,'stop','success','2026-04-24 10:22:36','2026-04-24 10:56:55',2059,NULL,1040217292,'/opt/media/bin/www/record/live/camera_192_168_1_64/2026-04-24/2026-04-24-10-22-05-0.mp4','2026-04-24 10:56:55','manual'),(25,1777001090381,1,1,'stop','success','2026-04-24 11:08:37','2026-04-24 11:24:50',973,NULL,491618065,'/opt/media/bin/www/record/live/camera_192_168_1_64/2026-04-24/2026-04-24-11-07-50-0.mp4','2026-04-24 11:24:50','manual'),(26,1777007582834,1,1,'stop','success','2026-04-24 12:59:34','2026-04-24 13:13:02',808,NULL,408366485,'/opt/media/bin/www/record/live/camera_192_168_1_64/2026-04-24/2026-04-24-12-07-40-0.mp4','2026-04-24 13:13:02','manual'),(27,1777007783081,1,1,'stop','success','2026-04-24 13:13:41','2026-04-24 13:16:23',162,NULL,81971741,'/opt/media/bin/www/record/live/camera_192_168_1_64/2026-04-24/2026-04-24-13-13-33-0.mp4','2026-04-24 13:16:23','manual'),(28,1777011386788,1,1,'stop','deleted','2026-04-24 13:16:25','2026-04-24 14:16:26',3601,NULL,1820006476,'/opt/media/bin/www/record/live/camera_192_168_1_64/2026-04-24/2026-04-24-13-16-26-0.mp4','2026-04-24 14:16:26','manual'),(29,1777011801041,1,1,'stop','success','2026-04-24 14:17:08','2026-04-24 14:23:21',373,NULL,188663072,'/opt/media/bin/www/record/live/camera_192_168_1_64/2026-04-24/2026-04-24-14-16-27-1.mp4','2026-04-24 14:23:21','manual'),(30,1777013451897,1,1,'stop','success','2026-04-24 14:26:00','2026-04-24 14:50:51',1491,NULL,753750652,'/opt/media/bin/www/record/live/camera_192_168_1_64/2026-04-24/2026-04-24-14-23-24-0.mp4','2026-04-24 14:50:51','manual');
/*!40000 ALTER TABLE `recording_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(255) NOT NULL COMMENT '密码（BCrypt加密）',
  `role` varchar(20) DEFAULT 'USER' COMMENT '角色：ADMIN/USER',
  `avatar` varchar(200) DEFAULT NULL COMMENT '头像URL',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`),
  KEY `idx_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user`
--

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;
INSERT INTO `user` VALUES (1,'admin','$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5E','ADMIN',NULL,'2026-04-13 16:58:20');
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-04-24 15:25:22

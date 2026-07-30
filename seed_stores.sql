USE trade_notes;

-- ===========================================
-- 七鲜超市门店数据
-- ===========================================
INSERT INTO stores (id, brand, name, address, start_time_online, end_time_online, start_time_offline, end_time_offline, longitude, latitude, status, created_time) VALUES
('seven_044d8c6e87424161bb4b2c1656ecd42b','seven','K11店','天津市滨海新区新城西路16号','08:00','21:00','09:00','21:30',117.703686,39.022621,0,NOW()),
('seven_05b4b9bfed324d27badb023aba570a87','seven','天河城店','天津市和平区和平路263号天河城购物中心B1层','08:00','22:00','10:00','22:00',117.203999,39.125830,0,NOW()),
('seven_1f79eb6d370f47c6b5de1bdf8d09879c','seven','青旅运动新天地店','天津市和平区友谊北路60号青旅运动新天地一层','08:00','21:00','08:00','21:00',117.200000,39.100000,0,NOW()),
('seven_2411d8cd3d054e4ba1a0db0f8f876eb2','seven','宝龙店','天津市滨海新区于家堡1813号宝龙广场1层','08:00','21:00','09:00','22:00',117.680102,39.007429,0,NOW()),
('seven_2ce36b33c6fb438fb6eece0c80eff647','seven','白堤路店','天津市南开区白堤路168号世通大厦一层','08:00','22:00','08:00','22:00',117.157653,39.116457,0,NOW()),
('seven_74827978258746f7a4cf0342b695395a','seven','乐宾百货店','天津市和平区滨江道18号乐宾百货B1层','08:00','21:30','10:00','21:30',117.195091,39.120818,0,NOW()),
('seven_7e8cfa7773634ab9bc61b4ddccbd2191','seven','鲁肎城店','天津市南开区天塔道与卫津南路交口鲁肎城负1层','08:00','21:00','10:00','22:00',117.175242,39.095419,0,NOW()),
('seven_830f51f4e3f04461ba593a74750f29e6','seven','保利广场店','天津市和平区福安大街保利广场负1层','08:00','21:00','09:00','21:30',117.189200,39.131044,0,NOW()),
('seven_9235fa99d89f4084857abacb926d519d','seven','金隅嘉品店','天津市滨海新区杭州道增1195号B1层','08:00','21:15','10:00','21:30',117.678092,39.032629,0,NOW()),
('seven_a0e90c94cf49485bbb67b39b41ac299a','seven','西青大悦汇店','天津市西青区中北镇万卉路5号西青大悦汇A座B1层','08:00','22:00','08:00','22:00',117.090730,39.140434,0,NOW())
ON DUPLICATE KEY UPDATE name=VALUES(name), address=VALUES(address);

-- ===========================================
-- 盒马鲜生门店数据
-- ===========================================
INSERT INTO stores (id, brand, name, address, start_time_online, end_time_online, start_time_offline, end_time_offline, longitude, latitude, status, created_time) VALUES
('hema_001','hema','盒马鲜生(奥体中心店)','天津市南开区卫津南路与宾水道交口天津奥林匹克中心B1层','09:00','22:00','10:00','22:00',117.1995,39.1073,0,NOW()),
('hema_002','hema','盒马鲜生(南开大悦城店)','天津市南开区南门外大街与大悦城购物中心B1层','09:00','22:00','10:00','22:00',117.1617,39.1275,0,NOW()),
('hema_004','hema','盒马鲜生(水游城店)','天津市红桥区大丰路18号鹏欣水游城B1层','09:00','21:30','10:00','21:30',117.1470,39.1560,0,NOW())
ON DUPLICATE KEY UPDATE name=VALUES(name), address=VALUES(address);

-- ===========================================
-- 山姆会员店门店数据
-- ===========================================
INSERT INTO stores (id, brand, name, address, start_time_online, end_time_online, start_time_offline, end_time_offline, longitude, latitude, status, created_time) VALUES
('sam_001','sam','山姆会员商店(梅江店)','天津市河西区解放南路与梅江道交口','08:00','22:00','08:00','22:00',117.2270,39.0668,0,NOW()),
('sam_002','sam','山姆会员商店(红桥店)','天津市西青区西青道与增吉路交口西北侧','08:00','22:00','08:00','22:00',117.1330,39.1510,1,NOW()),
('sam_003','sam','山姆会员商店(东丽店)','天津市东丽区津塘路与先锋路交口附近','08:00','22:00','08:00','22:00',117.3100,39.0850,1,NOW())
ON DUPLICATE KEY UPDATE name=VALUES(name), address=VALUES(address);

SELECT brand, COUNT(*) AS total FROM stores GROUP BY brand;
SELECT * FROM stores;

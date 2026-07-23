INSERT INTO UserInformation(user_id,user_name,user_account,user_email,user_password,user_status)
VALUES ('demo-user','Refine Demo','demo@refine.local','demo@refine.local',
        '$2a$12$7WC8PvDTwsOqJ44zNxtgD.NjwacXz6tkuVP.7tNCIFoJaWKkKgvXi',1)
ON DUPLICATE KEY UPDATE user_name=VALUES(user_name);


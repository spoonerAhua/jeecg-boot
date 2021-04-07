http://doc.jeecg.com/

docker run --name mysql_db_server --restart always -p 3306:3306 -e MYSQL_ROOT_HOST='%' -e MYSQL_ROOT_PASSWORD=root -d mysql:5.7.31
docker run --name mysql-adminer -d -p 33306:8080 adminer:4.8.0

docker run -it -d --name redis -p 6379:6379 redis:6.2.1
docker run -it -d --name redisAdmin --link redis:redis -p 16379:80 -e REDIS_1_HOST=redis -e REDIS_1_NAME=redis erikdubbelboer/phpredisadmin:v1.13.1

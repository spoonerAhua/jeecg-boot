http://doc.jeecg.com/

# Mysql

docker rm -f jeecg_mysql jeecg_mysql-adminer

docker run -d --name jeecg_mysql         -p  3306:3306 --restart always -e MYSQL_ROOT_HOST='%' -e MYSQL_ROOT_PASSWORD=root mysql:5.7.31 --lower_case_table_names=1

docker run -d --name jeecg_mysql-adminer -p 13306:8080 --restart always --link jeecg_mysql:db adminer:4.8.0

curl localhost:13306

# redis

docker rm -f jeecg_redis jeecg_redisAdmin

docker run -d --name jeecg_redis      -p  6379:6379 --restart always redis:6.2.1

docker run -d --name jeecg_redisAdmin -p 16379:80   --restart always --link jeecg_redis:redis  -e REDIS_1_HOST=jeecg_redis -e REDIS_1_NAME=jeecg_redis erikdubbelboer/phpredisadmin:v1.13.1

curl localhost:16379

# es6.8.13

docker run --restart always --name jeecg_es6 -it -d -p 9200:9200 -p 9300:9300 -e "discovery.type=single-node" docker.elastic.co/elasticsearch/elasticsearch:6.8.13

curl http://localhost:9200

docker run -d --name jeecg_es6_admin -p 9100:9100 mobz/elasticsearch-head:5

curl http://localhost:9100



# 在wsl中的docker中无法监控文件变化，不能够在修改后自动刷新页面，因此不用docker。 使用windows10的yarn安装版本。

# docker rm -f jeecg_node

# docker run -it -d -p 13000:3000 --name jeecg_node -w /jeecg -v /mnt/e/_git-source/github.com/spoonerAhua/jeecg-boot/ant-design-vue-jeecg:/jeecg node:12.22.1

# docker exec -it -w /jeecg jeecg_node bash -c "cd /jeecg & yarn add livereload -D & yarn install"

# docker exec -it -w /jeecg jeecg_node bash -c "cd /jeecg & yarn serve"




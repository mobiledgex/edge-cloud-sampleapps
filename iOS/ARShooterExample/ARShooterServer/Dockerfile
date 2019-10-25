FROM node:8

WORKDIR /usr/src/app/

COPY package*.json /usr/src/app/
RUN npm install
COPY . /usr/src/app

CMD node shooter-server.js 

EXPOSE 1337 

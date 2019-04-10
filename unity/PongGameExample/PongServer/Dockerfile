FROM node:8

WORKDIR /usr/src/app/

COPY package*.json /usr/src/app/
RUN npm install
COPY . /usr/src/app

CMD node yawsps.js

EXPOSE 3000



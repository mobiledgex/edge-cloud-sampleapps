FROM node:8

WORKDIR /tmp/app

COPY package.json /tmp/app
RUN npm install


COPY yawsps.js /tmp/app

CMD node yawsps.js

EXPOSE 3000



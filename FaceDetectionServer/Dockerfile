FROM python:3.6.6-onbuild
COPY requirements.txt /tmp
WORKDIR /tmp
RUN pip install -r requirements.txt
WORKDIR /usr/src/app/moedx
EXPOSE 8008/tcp
ENV PATH=/usr/local/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin

CMD ["gunicorn","moedx.wsgi:application","--bind","0.0.0.0:8008","--workers=10"]

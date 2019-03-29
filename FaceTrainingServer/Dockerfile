FROM python:3.6.6-onbuild
COPY requirements.txt /tmp
WORKDIR /tmp
RUN pip install -r requirements.txt
WORKDIR /usr/src/app/facerec
EXPOSE 8009/tcp
ENV PATH=/usr/local/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin

CMD ["gunicorn","facerec.wsgi:application","--bind","0.0.0.0:8009","--workers=1"]

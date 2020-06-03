### To run the server manually:
```bash
virtualenv env -ppython3
source env/bin/activate
pip install -r requirements.txt
cd facerec/
python manage.py makemigrations trainer
python manage.py migrate
gunicorn facerec.wsgi:application --bind 0.0.0.0:8009
```

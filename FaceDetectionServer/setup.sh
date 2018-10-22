virtualenv env -ppython3
source env/bin/activate
pip install {django,djangorestframework,gunicorn,opencv-python,pillow}
export DJANGO_SETTINGS_MODULE=moedx.moedx.settings

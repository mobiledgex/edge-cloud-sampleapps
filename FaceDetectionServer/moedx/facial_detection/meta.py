import sys

G_LOGGING = True
DEV_NULL = open('/dev/null', 'w')

def logging(fun):
    """ 
    Decorator that switches printing from stdout during 
    logging to dev/null when logging is disabled. Include
    file=sys.stdout in the kwarg declerations and include
    in print statements with print(..., file=file).
    """
    def _logging(*args, **kwargs):
        if G_LOGGING:
            kwargs['file'] = sys.stdout
        else:
            kwargs['file'] = DEV_NULL
            
        return fun(*args, **kwargs)
    return _logging
    

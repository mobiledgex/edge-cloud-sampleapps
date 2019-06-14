import socket
import time
import json
import base64
import struct
import time

# Globals
total_latency_full_process = 0
count_latency_full_process = 0
total_latency_network_only = 0
count_latency_network_only = 0

do_server_stats = False
show_responses = False

def time_open_socket(host, port):
    port = 8008
    global total_latency_network_only
    global count_latency_network_only
    now = time.time()
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM);
    sock.settimeout(2)
    result = sock.connect_ex((host,port))
    if result != 0:
        print("Could not connect to %s on port %d" %(host, port))
        return
    millis = (time.time() - now)*1000
    elapsed = "%.3f" %millis
    print("%s ms to open socket" %(elapsed))
    total_latency_network_only = total_latency_network_only + millis
    count_latency_network_only += 1

def time_rtt(sock, host, port):
    global total_latency_network_only
    global count_latency_network_only
    opcode = bytes('ping_rtt', 'utf-8')
    now = time.time()
    sock.sendall(struct.pack('!8s', opcode))
    received = str(sock.recv(1024), "utf-8")
    print("RTT received:", received)
    millis = (time.time() - now)*1000
    elapsed = "%.3f" %millis
    if show_responses:
        print("%s ms RTT" %(elapsed))
    total_latency_network_only = total_latency_network_only + millis
    count_latency_network_only += 1

def recvall(sock, count):
    buf = b''
    while count:
        newbuf = sock.recv(count)
        if not newbuf: return None
        buf += newbuf
        count -= len(newbuf)
    return buf

def run_multi(num_repeat, host, port, op_code, image_file_name, thread_name):
    global do_server_stats
    global show_responses
    global total_latency_full_process
    global count_latency_full_process
    # print("run_multi(%s, %s)\n" %(host, image_file_name))
    with open(image_file_name, "rb") as f:
        image = f.read()
    print("len", len(image))
    data = image
    # if op_code == 'ping_rtt':
    #     data = b'NONE'

    # Open the connection one time, then send multiple packets
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.connect((host, port))

    print("%s Starting" %thread_name)
    print("repeating %s %d times" %(op_code, num_repeat))
    for x in range(num_repeat):
        now = time.time()
        length = len(data)
        # opcode = bytes(op_code, 'utf-8')
        # sock.sendall(struct.pack('!8s', opcode))
        sock.sendall(struct.pack('!I', op_code))
        sock.sendall(struct.pack('!I', length))
        sock.sendall(data)
        # sock.settimeout(2)

        # lengthbuf = recvall(sock, 4)
        # length, = struct.unpack('!I', lengthbuf)
        # received = str(recvall(sock, length), "utf-8")

        lengthbuf = sock.recv(4)
        length, = struct.unpack('!I', lengthbuf)
        received = str(sock.recv(length), "utf-8")

        # received = str(sock.recv(1024), "utf-8")

        millis = (time.time() - now)*1000
        total_latency_full_process = total_latency_full_process + millis
        count_latency_full_process += 1
        elapsed = "%.3f" %millis
        if show_responses:
            print("%s ms to send and receive: %s" %(elapsed, received))

        if x % 4 == 0:
            time_open_socket(host, port)
        # time_rtt(sock, host, port)
        # time.sleep(1)
    print("%s Done" %thread_name)

if __name__ == "__main__":
    import sys
    from threading import Thread
    import argparse
    parser = argparse.ArgumentParser()

    parser.add_argument("-s", "--server", required=True, help="Server host name or IP address.")
    parser.add_argument("-o", "--opcode", type=int, required=True, help="Opcode to process, e.g. 1=face_det, 2=face_rec")
    parser.add_argument("-f", "--filename", required=True, help="Name of image file to send.")
    parser.add_argument("-r", "--repeat", type=int, default=1, help="Number of times to repeat.")
    parser.add_argument("-t", "--threads", type=int, default=1, help="Number of concurent execution threads.")
    parser.add_argument("-p", "--port", type=int, default=8011, help="Port number")
    parser.add_argument("--show-responses", action='store_true', help="Show responses.")
    parser.add_argument("--server-stats", action='store_true', help="Get server stats every Nth frame.")
    args = parser.parse_args()

    do_server_stats = args.server_stats
    show_responses = args.show_responses

    opcodes = {0:'server_response', 1:'face_det', 2:'face_rec', 3:'pose_det'}
    print(opcodes)

    for i in range(args.threads):
        thread = Thread(target=run_multi, args=(args.repeat, args.server, args.port, args.opcode, args.filename, "thread-%d" %i))
        thread.start()

    # Wait for the last thread to finish.
    thread.join()

    if count_latency_full_process > 0:
        average_latency_full_process = total_latency_full_process / count_latency_full_process
        print("===> Average Latency Full Process=%.3f ms" %average_latency_full_process)
    if count_latency_network_only > 0:
        average_latency_network_only = total_latency_network_only / count_latency_network_only
        print("===> Average Latency Network Only=%.3f ms" %average_latency_network_only)

import socket
import time
import json
import base64

# Globals
total_latency_full_process = 0
count_latency_full_process = 0
total_latency_network_only = 0
count_latency_network_only = 0

do_server_stats = False
show_responses = False

def udp_client_base64(ip, port, data):
    # print(ip, port, data)

    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    total_latency = 0
    count = 10
    for x in range(count):
        now = time.time()
        sock.sendto(bytes(data, "utf-8"), (HOST, PORT))
        received = str(sock.recv(1024), "utf-8")
        millis = (time.time() - now)*1000
        total_latency += millis
        elapsed = "%.3f" %millis
        print("%s ms to send and receive" %(elapsed))
        # print("Sent:     {}".format(data))
        print("Received: {}".format(received))
    print("Average latency=%s" %("%.3f" %(total_latency/count)))

def udp_client_binary(ip, port, data):
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    total_latency = 0
    count = 10
    for x in range(count):
        now = time.time()
        sock.sendto(data, (HOST, PORT))
        received = str(sock.recv(1024), "utf-8")
        millis = (time.time() - now)*1000
        total_latency += millis
        elapsed = "%.3f" %millis
        print("%s ms to send and receive" %(elapsed))
        # print("Sent:     {}".format(data))
        print("Received: {}".format(received))
    print("Average latency=%s" %("%.3f" %(total_latency/count)))

def run_multi(num_repeat, host, port, image_file_name, thread_name):
    global do_server_stats
    global show_responses
    global total_latency_full_process
    global count_latency_full_process
    # print("run_multi(%s, %s)\n" %(host, image_file_name))
    with open(image_file_name, "rb") as f:
        image = f.read()
    print("len", len(image))
    data = image
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)

    print("%s Starting" %thread_name)
    for x in range(num_repeat):
        now = time.time()
        sock.sendto(data, (host, port))
        sock.settimeout(2)
        received = str(sock.recv(1024), "utf-8")
        millis = (time.time() - now)*1000
        total_latency_full_process = total_latency_full_process + millis
        count_latency_full_process += 1
        elapsed = "%.3f" %millis
        if show_responses:
            print("%s ms to send and receive: %s" %(elapsed, received))
        # print("Sent:     {}".format(data))
        # print("Received: {}".format(received))
        # if x % 4 == 0:
        #     if do_server_stats:
        #         get_server_stats(host)
        #     time_open_socket(host, 8008)
    print("%s Done" %thread_name)

if __name__ == "__main__":
    import sys
    from threading import Thread
    import argparse
    parser = argparse.ArgumentParser()

    parser.add_argument("-s", "--server", required=True, help="Server host name or IP address.")
    parser.add_argument("-f", "--filename", required=True, help="Name of image file to send.")
    parser.add_argument("-r", "--repeat", type=int, default=1, help="Number of times to repeat.")
    parser.add_argument("-t", "--threads", type=int, default=1, help="Number of concurent execution threads.")
    parser.add_argument("-p", "--port", type=int, default=8010, help="Port number")
    parser.add_argument("--show-responses", action='store_true', help="Show responses.")
    parser.add_argument("--server-stats", action='store_true', help="Get server stats every Nth frame.")
    args = parser.parse_args()

    do_server_stats = args.server_stats
    show_responses = args.show_responses

    for i in range(args.threads):
        thread = Thread(target=run_multi, args=(args.repeat, args.server, args.port, args.filename, "thread-%d" %i))
        thread.start()

    # Wait for the last thread to finish.
    thread.join()

    if count_latency_full_process > 0:
        average_latency_full_process = total_latency_full_process / count_latency_full_process
        print("Average Latency Full Process=%.3f ms" %average_latency_full_process)
    if count_latency_network_only > 0:
        average_latency_network_only = total_latency_network_only / count_latency_network_only
        print("Average Latency Network Only=%.3f ms" %average_latency_network_only)

    """
    with open(image_file_name, "rb") as f:
        image = base64.b64encode(f.read()).decode("utf-8")

    x = 1
    data = {'image': str(image), "subject":"Bobo", "message":"Hello World-%d" %x}
    json_data = json.dumps(data)
    print("len", len(json_data))
    udp_client_base64(HOST, PORT, json_data)
    """

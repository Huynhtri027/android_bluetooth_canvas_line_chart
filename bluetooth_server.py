# coding: utf-8
import bluetooth  # need pybluez installed through pip
                  # may need libbluetooth-dev installed through apt-get
import time
port = 1
uuid = "00001101-0000-1000-8000-00805F9B34FB"    # should be consistent in server and client
server_sock=bluetooth.BluetoothSocket( bluetooth.RFCOMM )
server_sock.bind(("",port))
server_sock.listen(1)
bluetooth.advertise_service( server_sock, "FooBar Service", uuid )
client_sock,address = server_sock.accept()
print "connect finish"
#because of accuracy problem, the number received by the phone may be slightly different
while True:         
	client_sock.send("1.1")
	time.sleep(3)
	client_sock.send("1.5")
	time.sleep(3)
	client_sock.send("12.2")
	time.sleep(3)
	client_sock.send("-13.5")
	time.sleep(3)
	client_sock.send("11.5")
	time.sleep(3)



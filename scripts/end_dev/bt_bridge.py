#!/usr/bin/env python3
"""
라즈베리 파이 End_Dev Bluetooth 브릿지.

스마트폰 Serial Bluetooth Terminal -> RFCOMM -> 이 스크립트 -> Java(9200)

사용:
  pip install pybluez
  sudo python3 bt_bridge.py

스마트폰에서 전송 예:
  HELP
  LIST
  INSERT:500
  BUY:0
  RETURN
  STATUS
"""

import socket
import sys

JAVA_HOST = "127.0.0.1"
JAVA_PORT = 9200
BT_PORT = 1  # RFCOMM channel


def forward_to_java(command: str) -> str:
    with socket.create_connection((JAVA_HOST, JAVA_PORT), timeout=5) as sock:
        sock.sendall((command.strip() + "\n").encode("utf-8"))
        data = sock.recv(1024)
        return data.decode("utf-8", errors="replace").strip()


def run_bt_server():
    try:
        import bluetooth
    except ImportError:
        print("PyBluez 필요: pip install pybluez")
        sys.exit(1)

    server_sock = bluetooth.BluetoothSocket(bluetooth.RFCOMM)
    server_sock.bind(("", BT_PORT))
    server_sock.listen(1)
    print(f"[BT] RFCOMM channel {BT_PORT} 대기 중...")
    print(f"[BT] Java 브릿지 -> {JAVA_HOST}:{JAVA_PORT}")

    while True:
        client_sock, client_info = server_sock.accept()
        print(f"[BT] 연결: {client_info}")
        try:
            while True:
                data = client_sock.recv(1024)
                if not data:
                    break
                cmd = data.decode("utf-8", errors="replace").strip()
                if not cmd:
                    continue
                print(f"[BT] 수신: {cmd}")
                try:
                    response = forward_to_java(cmd)
                except OSError as e:
                    response = f"ERR:JAVA:{e}"
                client_sock.send((response + "\n").encode("utf-8"))
                print(f"[BT] 응답: {response}")
        finally:
            client_sock.close()


def run_tcp_test_mode():
    """Bluetooth 없이 PC 테스트: 표준입력 -> Java"""
    print(f"[TEST] stdin -> {JAVA_HOST}:{JAVA_PORT}")
    for line in sys.stdin:
        cmd = line.strip()
        if not cmd:
            continue
        print(forward_to_java(cmd))


if __name__ == "__main__":
    if len(sys.argv) > 1 and sys.argv[1] == "--test":
        run_tcp_test_mode()
    else:
        run_bt_server()

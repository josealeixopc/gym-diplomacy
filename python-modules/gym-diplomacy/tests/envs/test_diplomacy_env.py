# Import packages to be tested
from gym_diplomacy.envs import diplomacy_env

# Import general packages for testing
import unittest
import typing

# Import specific dependencies for test setup
import socketserver
import socket
import threading


class TestHandler(socketserver.BaseRequestHandler):
    data: bytes
    threads: typing.List[threading.Thread] = []

    def handle(self):
        self.data = self.request.recv(1024).strip()
        self.threads.append(threading.current_thread())

        response: bytes = self.data.upper()
        self.request.sendall(response)


def client(ip: str, port: int, message: bytes) -> bytes:
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
        sock.connect((ip, port))
        sock.sendall(message)
        response = sock.recv(1024)
        return response


# Testing creation and initialization
class MyLocalThreadedTCPServerInitTestCase(unittest.TestCase):
    def test_initialize_with_port(self):
        test_port: int = 5000
        default_host: str = 'localhost'
        test_address: typing.Tuple[str, int] = (socket.gethostbyname(default_host), test_port)

        server = diplomacy_env.DiplomacyThreadedTCPServer(None, test_port, TestHandler)
        with server:
            server_thread = threading.Thread(target=server.serve_forever)
            # Exit the server thread when the main thread terminates
            server_thread.daemon = True
            server_thread.start()

            self.assertEqual(server.port, test_port)
            self.assertEqual(server.host, default_host)
            self.assertEqual(server.server_address, test_address)

            server.shutdown()

    def test_initialize_with_port_and_host(self):
        test_port: int = 5000
        test_host: str = 'localhost'
        test_address: typing.Tuple[str, int] = (socket.gethostbyname(test_host), test_port)

        server = diplomacy_env.DiplomacyThreadedTCPServer(None, test_port, TestHandler)

        with server:
            server_thread = threading.Thread(target=server.serve_forever)
            # Exit the server thread when the main thread terminates
            server_thread.daemon = True
            server_thread.start()

            self.assertEqual(server.port, test_port)
            self.assertEqual(server.host, test_host)
            self.assertEqual(server.server_address, test_address)

            server.shutdown()


# Testing the class itself
class MyLocalThreadedTCPServerTestCase(unittest.TestCase):
    server: diplomacy_env.DiplomacyThreadedTCPServer

    def setUp(self) -> None:
        test_port: int = 5000
        self.server = diplomacy_env.DiplomacyThreadedTCPServer(None, test_port, TestHandler)

        server_thread = threading.Thread(target=self.server.serve_forever)
        # Exit the server thread when the main thread terminates
        server_thread.daemon = True
        server_thread.start()

    def test_handle_one_client(self):
        test_message: bytes = b'Hello, world'
        ip, port = self.server.server_address

        response: bytes = client(ip, port, test_message)

        self.assertEqual(test_message.upper(), response)

    def test_handle_three_clients(self):
        test_message1: bytes = b'Hello, world'
        test_message2: bytes = b'How are you?'
        test_message3: bytes = b'Fine! Thanks :D'

        ip, port = self.server.server_address

        response1: bytes = client(ip, port, test_message1)
        response2: bytes = client(ip, port, test_message2)
        response3: bytes = client(ip, port, test_message3)

        self.assertEqual(test_message1.upper(), response1)
        self.assertEqual(test_message2.upper(), response2)
        self.assertEqual(test_message3.upper(), response3)

    # noinspection PyUnresolvedReferences
    def test_handle_clients_in_separate_threads(self):
        test_message1: bytes = b'Hello, world'
        test_message2: bytes = b'How are you?'
        test_message3: bytes = b'Fine! Thanks :D'

        ip, port = self.server.server_address

        client(ip, port, test_message1)
        client(ip, port, test_message2)
        client(ip, port, test_message3)

        are_all_threads_same = all(
            x == self.server.handler_request_class.threads[0] for x in self.server.handler_request_class.threads)

        self.assertFalse(are_all_threads_same)
        self.assertEqual(3, len(self.server.handler_request_class.threads))

    def tearDown(self) -> None:
        with self.server:
            self.server.shutdown()

    class DiplomacyTCPHandlerTestCase(unittest.TestCase):
        server_thread: threading.Thread
        host: str = "localhost"
        port: int = 9999

        def setUp(self) -> None:
            # Create the server, binding to localhost on port 9999
            # bind_and_activate determines whether the constructor tries to bind and activate the socket right away
            # because we want to define the allow_reuse_address flag, we set it to false
            self.server = socketserver.TCPServer((self.host, self.port), diplomacy_env.DiplomacyTCPHandler,
                                                 bind_and_activate=False)
            self.server.allow_reuse_address = True
            self.server.server_bind()
            self.server.server_activate()

            # Activate the server; this will keep running until you
            # interrupt the program with Ctrl-C
            self.server_thread = threading.Thread(target=self.server.serve_forever)
            # Exit the server thread when the main thread terminates
            self.server_thread.daemon = True
            self.server_thread.start()

        def test_handler(self) -> None:
            test_message: bytes = b'Hello, world'

            with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
                s.connect((self.host, self.port))
                s.sendall(test_message)
                data = s.recv(1024)

                self.assertEqual(data, test_message.upper())

        def tearDown(self) -> None:
            self.server.shutdown()
            self.server.server_close()


class DiplomacyEnvTestCase(unittest.TestCase):
    diplomacy_env: diplomacy_env.DiplomacyEnv

    def setUp(self) -> None:
        self.diplomacy_env = diplomacy_env.DiplomacyEnv()

    def tearDown(self) -> None:
        self.diplomacy_env.close()

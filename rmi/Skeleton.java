package rmi;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

/** RMI skeleton
    <p>
    A skeleton encapsulates a multi threaded TCP server. The server's clients are
    intended to be RMI stubs created using the <code>Stub</code> class.

    <p>
    The skeleton class is parameterized by a type variable. This type variable
    should be instantiated with an interface. The skeleton will accept from the
    stub requests for calls to the methods of this interface. It will then
    forward those requests to an object. The object is specified when the
    skeleton is constructed, and must implement the remote interface. Each
    method in the interface should be marked as throwing
    <code>RMIException</code>, in addition to any other exceptions that the user
    desires.

    <p>
    Exceptions may occur at the top level in the listening and service threads.
    The skeleton's response to these exceptions can be customized by deriving
    a class from <code>Skeleton</code> and overriding <code>listen_error</code>
    or <code>service_error</code>.
 */
public class Skeleton<T>
{
	private InetSocketAddress inet;
	private T server;
	private ServerSocket ss;
	private int port = 1000;
	public static Map<InetSocketAddress, Skeleton> addressMap = new HashMap<>();
	private ListenThread listenThread;
	public boolean isRunning;
	private Class<T> inter;
	/** Creates a <code>Skeleton</code> with no initial server address. The
        address will be determined by the system when <code>start</code> is
        called. Equivalent to using <code>Skeleton(null)</code>.

        <p>
        This constructor is for skeletons that will not be used for
        bootstrapping RMI - those that therefore do not require a well-known
        port.

        @param c An object representing the class of the interface for which the
                 skeleton server is to handle method call requests.
        @param server An object implementing said interface. Requests for method
                      calls are forwarded by the skeleton to this object.
        @throws Error If <code>c</code> does not represent a remote interface -
                      an interface whose methods are all marked as throwing
                      <code>RMIException</code>.
        @throws NullPointerException If either of <code>c</code> or
                                     <code>server</code> is <code>null</code>.
	 */
	public Skeleton(Class<T> c, T server) {
		this(c, server, null);
	}

	/** Creates a <code>Skeleton</code> with the given initial server address.

        <p>
        This constructor should be used when the port number is significant.

        @param c An object representing the class of the interface for which the
                 skeleton server is to handle method call requests.
        @param server An object implementing said interface. Requests for method
                      calls are forwarded by the skeleton to this object.
        @param address The address at which the skeleton is to run. If
                       <code>null</code>, the address will be chosen by the
                       system when <code>start</code> is called.
        @throws Error If <code>c</code> does not represent a remote interface -
                      an interface whose methods are all marked as throwing
                      <code>RMIException</code>.
        @throws NullPointerException If either of <code>c</code> or
                                     <code>server</code> is <code>null</code>.
	 */
	public Skeleton(Class<T> c, T server, InetSocketAddress address) {
		System.out.println("without address:"+address);
		if(c==null || server == null)
			throw new NullPointerException("Either c or server is null");
		if(!c.isInterface())
			throw new Error("Class is not an interface");
		remoteInterfaceCheck(c);
		try {
			this.ss = new ServerSocket();
		} catch (IOException e) {
			System.out.println("IOEXCEPTION in construcor Skeleton:"+e.getMessage());
		}
		this.inter = c;
		this.inet = address;
		setServer(server);
		addressMap.put(inet, this); 
	}

	/** Called when the listening thread exits.

        <p>
        The listening thread may exit due to a top-level exception, or due to a
        call to <code>stop</code>.

        <p>
        When this method is called, the calling thread owns the lock on the
        <code>Skeleton</code> object. Care must be taken to avoid deadlocks when
        calling <code>start</code> or <code>stop</code> from different threads
        during this call.

        <p>
        The default implementation does nothing.

        @param cause The exception that stopped the skeleton, or
                     <code>null</code> if the skeleton stopped normally.
	 */
	protected void stopped(Throwable cause)
	{
	}

	/** Called when an exception occurs at the top level in the listening
        thread.

        <p>
        The intent of this method is to allow the user to report exceptions in
        the listening thread to another thread, by a mechanism of the user's
        choosing. The user may also ignore the exceptions. The default
        implementation simply stops the server. The user should not use this
        method to stop the skeleton. The exception will again be provided as the
        argument to <code>stopped</code>, which will be called later.

        @param exception The exception that occurred.
        @return <code>true</code> if the server is to resume accepting
                connections, <code>false</code> if the server is to shut down.
	 */
	protected boolean listen_error(Exception exception) {
		System.out.print(exception.getMessage());
		return false;
	}

	/** Called when an exception occurs at the top level in a service thread.

        <p>
        The default implementation does nothing.

        @param exception The exception that occurred.
	 */
	protected void service_error(RMIException exception) { 
		System.out.print(exception.getMessage());
	}

	/** Starts the skeleton server.

        <p>
        A thread is created to listen for connection requests, and the method
        returns immediately. Additional threads are created when connections are
        accepted. The network address used for the server is determined by which
        constructor was used to create the <code>Skeleton</code> object.

        @throws RMIException When the listening socket cannot be created or
                             bound, when the listening thread cannot be created,
                             or when the server has already been started and has
                             not since stopped.
	 */
	public synchronized void start() throws RMIException {
		try {
			this.ss.bind(inet);
			if (this.inet == null) {
				this.inet = (InetSocketAddress) ss.getLocalSocketAddress();}
			System.out.println("\n PORT:"+inet.getPort());
			//ss = new ServerSocket(inet.getPort());
			System.out.println("START:"+inet);
			listenThread = new ListenThread();
			listenThread.start();
			isRunning = true;
		} catch(Exception e ){
			System.out.println("IOException when starting server: "+e.getMessage());
		}
		//throw new UnsupportedOperationException("not implemented");
	}

	/**
	 * This thread listens for server socket connection
	 *
	 */
	private class ListenThread extends Thread {
		@Override
		public void run() {
			try {
				while (isRunning) {
					Socket clientSocket = ss.accept();
					Client client = new Client(clientSocket);
					Thread th = new Thread(client);
					th.start();
				}
			} catch (IOException e) {

			}
		}
	}

	/**
	 * 
	 * This writes and reads from and to the client
	 *
	 */
	private class Client extends Thread {
		Socket clientSocket;
		ObjectInputStream in = null;
		ObjectOutputStream out = null;
		public Client(Socket clientSocket) {
			this.clientSocket = clientSocket;
		}
		public void run() {
			try {
				if (isRunning) {
					this.out = new ObjectOutputStream(this.clientSocket.getOutputStream());
					this.out.flush();
					this.in = new ObjectInputStream(this.clientSocket.getInputStream());

					String methodName = (String) in.readObject();
					Class[] argTypes = (Class[]) in.readObject();
					Object[] args = (Object[]) in.readObject();
					Method m = inter.getDeclaredMethod(methodName, argTypes);
					Object result = m.invoke(server, args);
					out.writeObject(true);
					out.writeObject(result);
					clientSocket.close();
				}
			} catch (Exception e) {
				try {
					out.writeObject(false);
					out.writeObject(e.getCause());
					clientSocket.close();
				} catch (IOException e1) {
					System.out.println("Error in writing exception to user."+e1.getMessage());
				}
			}
		}
	}

	/** Stops the skeleton server, if it is already running.

        <p>
        The listening thread terminates. Threads created to service connections
        may continue running until their invocations of the <code>service</code>
        method return. The server stops at some later time; the method
        <code>stopped</code> is called at that point. The server may then be
        restarted.
	 */
	public synchronized void stop() {
		if(listenThread == null) {
			System.out.println("No thread present");
			return;
		}
		try {
			ss.close();
			isRunning = false;
			stopped(null);
			addressMap.remove(inet);
		} catch (Exception e) {
			System.out.println("Exception in stop method of Skeleton:"+e.getMessage());
		}
		//throw new UnsupportedOperationException("not implemented");
	}
	
	/**
	 * This method sets the server
	 * @param server
	 */
	public void setServer(T server) {
		this.server = server;
	}
	
	/**
	 * This method returns the server
	 * @return server
	 */
	public T getServer() {
		return this.server;
	}

    /**
     * This method checks if class c represents a remote interface such that all methods throw RMIException
     *
     * @param c interface that is associated with this Skeleton
     */
	private void remoteInterfaceCheck(Class<T> c) {
		Method[] methods = c.getDeclaredMethods();
		for(Method method : methods) {
			Class<?>[] exceptions = method.getExceptionTypes();
			boolean hasRMIException = false;
			for(Class<?> exception : exceptions) {
				if(exception.toString().equals("class rmi.RMIException")) {
					hasRMIException = true;
					break;
				}
			}
			if(!hasRMIException){
				throw new Error("Interface is not a remote interface");
			}
		}
	}
	
	/**
	 * This method returns the InetSocketaddress
	 * @return InetSocketaddress
	 */
	public InetSocketAddress getInetAddress() {
		return inet;
	}
}
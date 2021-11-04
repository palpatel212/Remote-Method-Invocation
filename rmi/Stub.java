package rmi;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.lang.reflect.*;
import java.lang.reflect.Proxy;

/** RMI stub factory.

    <p>
    RMI stubs hide network communication with the remote server and provide a
    simple object-like interface to their users. This class provides methods for
    creating stub objects dynamically, when given pre-defined interfaces.

    <p>
    The network address of the remote server is set when a stub is created, and
    may not be modified afterwards. Two stubs are equal if they implement the
    same interface and carry the same remote server address - and would
    therefore connect to the same skeleton. Stubs are serializable.
 */
public abstract class Stub
{
	/** Creates a stub, given a skeleton with an assigned adress.

        <p>
        The stub is assigned the address of the skeleton. The skeleton must
        either have been created with a fixed address, or else it must have
        already been started.

        <p>
        This method should be used when the stub is created together with the
        skeleton. The stub may then be transmitted over the network to enable
        communication with the skeleton.

        @param c A <code>Class</code> object representing the interface
                 implemented by the remote object.
        @param skeleton The skeleton whose network address is to be used.
        @return The stub created.
        @throws IllegalStateException If the skeleton has not been assigned an
                                      address by the user and has not yet been
                                      started.
        @throws UnknownHostException When the skeleton address is a wildcard and
                                     a port is assigned, but no address can be
                                     found for the local host.
        @throws NullPointerException If any argument is <code>null</code>.
        @throws Error If <code>c</code> does not represent a remote interface
                      - an interface in which each method is marked as throwing
                      <code>RMIException</code>, or if an object implementing
                      this interface cannot be dynamically created.
	 */
	public static <T> T create(Class<T> c, Skeleton<T> skeleton)
			throws UnknownHostException
	{
		if(c.equals(null) || skeleton.equals(null))
			throw new NullPointerException();
		InetSocketAddress address = skeleton.getInetAddress();
		if (address == null) {
			throw new IllegalStateException("Server is not running");
		}   	
		if (address.isUnresolved()){
			throw new UnknownHostException(" No address is found for the local host");
		}
		if (!c.isInterface()) {
			throw new Error("Class is not interface");
		}
		remoteInterfaceCheck(c);
		T newStub = (T)Proxy.newProxyInstance(c.getClassLoader(), new Class[] {c}, new StubProxy(skeleton.getServer(), skeleton));
		return newStub;   
	}

    /**
     * This method checks if class c represents a remote interface such that all methods throw RMIException
     *
     * @param c interface that is associated with this Skeleton
     */
	private static<T> void remoteInterfaceCheck(Class<T> c) {
		Method[] methods = c.getDeclaredMethods();
		for(Method method : methods) {
			Class<?>[] exceptions = method.getExceptionTypes();
			boolean hasRMIExcept = false;
			for(Class<?> exception : exceptions) {
				if(exception.toString().equals("class rmi.RMIException")) {
					hasRMIExcept = true;
					break;
				}
			}
			if(!hasRMIExcept){
				throw new Error("Interface is not a remote interface");
			}
		}
	}

	/** Creates a stub, given a skeleton with an assigned address and a hostname
        which overrides the skeleton's hostname.

        <p>
        The stub is assigned the port of the skeleton and the given hostname.
        The skeleton must either have been started with a fixed port, or else
        it must have been started to receive a system-assigned port, for this
        method to succeed.

        <p>
        This method should be used when the stub is created together with the
        skeleton, but firewalls or private networks prevent the system from
        automatically assigning a valid externally-routable address to the
        skeleton. In this case, the creator of the stub has the option of
        obtaining an externally-routable address by other means, and specifying
        this hostname to this method.

        @param c A <code>Class</code> object representing the interface
                 implemented by the remote object.
        @param skeleton The skeleton whose port is to be used.
        @param hostname The hostname with which the stub will be created.
        @return The stub created.
        @throws IllegalStateException If the skeleton has not been assigned a
                                      port.
        @throws NullPointerException If any argument is <code>null</code>.
        @throws Error If <code>c</code> does not represent a remote interface
                      - an interface in which each method is marked as throwing
                      <code>RMIException</code>, or if an object implementing
                      this interface cannot be dynamically created.
	 */
	public static <T> T create(Class<T> c, Skeleton<T> skeleton,
			String hostname)
	{
		if((skeleton.getInetAddress() == null) || skeleton.getInetAddress().getPort() == 0)
			throw new IllegalArgumentException("No port assigned to skeleton");
		if (c==null || skeleton==null || hostname==null)
			throw new NullPointerException("Argument is null");
		remoteInterfaceCheck(c);
		T newStub = (T)Proxy.newProxyInstance(c.getClassLoader(), new Class[] {c}, new StubProxy(skeleton.getServer(), skeleton));

		return newStub;
		//throw new UnsupportedOperationException("not implemented");
	}

	/** Creates a stub, given the address of a remote server.

        <p>
        This method should be used primarily when bootstrapping RMI. In this
        case, the server is already running on a remote host but there is
        not necessarily a direct way to obtain an associated stub.

        @param c A <code>Class</code> object representing the interface
                 implemented by the remote object.
        @param address The network address of the remote skeleton.
        @return The stub created.
        @throws NullPointerException If any argument is <code>null</code>.
        @throws Error If <code>c</code> does not represent a remote interface
                      - an interface in which each method is marked as throwing
                      <code>RMIException</code>, or if an object implementing
                      this interface cannot be dynamically created.
	 */
	public static <T> T create(Class<T> c, InetSocketAddress address)
	{
		if(c.equals(null) || address.equals(null))
			throw new NullPointerException();
		try {
			Socket socket = new Socket(address.getHostName(), address.getPort());
		} catch (UnknownHostException e) {
			System.out.println("Unknown Host Exception: "+e.getMessage());
		} catch (IOException e) {
			System.out.println("IOException: "+e.getMessage());
		}
		if (!c.isInterface()) {
			throw new Error("Class is not interface");
		}
		remoteInterfaceCheck(c);
		T newStub = null;
		if(!Skeleton.addressMap.containsKey(address)) {
			newStub = (T)Proxy.newProxyInstance(c.getClassLoader(), new Class[] {c}, new StubProxy());
		} else {
			Skeleton skeleton = Skeleton.addressMap.get(address);
			newStub = (T)Proxy.newProxyInstance(c.getClassLoader(), new Class[] {c}, new StubProxy(skeleton.getServer(), skeleton));
		}
		return newStub;
	}
}
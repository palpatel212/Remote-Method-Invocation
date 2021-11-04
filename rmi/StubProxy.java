package rmi;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

public class StubProxy implements InvocationHandler{

	Object target;
	Skeleton<?> skeleton;

	public StubProxy(){
		target = null;
		skeleton = null;
	}

	public StubProxy(Object object, Skeleton<?> skeleton){
		target = object;
		this.skeleton = skeleton;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

		//The invocation handler can deal with .equals(), .hashCode(), and .toString() by itself
		if(method.getName().equals("equals")) {
			return isEqual(args);
		}
		if(method.getName().equals("hashCode")) {
			return Objects.hash(skeleton);
		}
		if(method.getName().equals("toString")) {
			return (this.toString());
		}
		if (skeleton == null || target == null) {
			throw new RMIException("No skeleton or target");
		}

		//Invoke the method. Pass along any exceptions
		try {
			return method.invoke(target, args);
		}
		catch (InvocationTargetException e) {
			throw e.getTargetException();
		}
	}
	/**
	 * Determines if 2 objects are same or not
	 *
	 * @param args object to be compared
	 * @return true if both objects are same
	 */
	private boolean isEqual(Object[] args) {
		if(args[0] == null) 
			return false;
		if(args[0].equals(skeleton)) 
			return true;
		else
			return false;   
	}
}
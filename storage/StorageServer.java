package storage;

import java.io.*;
import java.net.*;

import common.*;
import rmi.*;
import naming.*;

/** Storage server.

    <p>
    Storage servers respond to client file access requests. The files accessible
    through a storage server are those accessible under a given directory of the
    local filesystem.
 */
public class StorageServer implements Storage, Command
{
	File root;
	private Skeleton<Storage> storageSkeleton;

	private Skeleton<Command> commandSkeleton;
	/** Creates a storage server, given a directory on the local filesystem.

        @param root Directory on the local filesystem. The contents of this
                    directory will be accessible through the storage server.
        @throws NullPointerException If <code>root</code> is <code>null</code>.
	 */
	public StorageServer(File root)
	{
		if(root.equals(null))
			throw new NullPointerException("The root is null");
		this.root = root;

		storageSkeleton = new Skeleton<>(Storage.class, this);
		commandSkeleton = new Skeleton<>(Command.class, this);

		//throw new UnsupportedOperationException("not implemented");
	}

	/** Starts the storage server and registers it with the given naming
        server.

        @param hostname The externally-routable hostname of the local host on
                        which the storage server is running. This is used to
                        ensure that the stub which is provided to the naming
                        server by the <code>start</code> method carries the
                        externally visible hostname or address of this storage
                        server.
        @param naming_server Remote interface for the naming server with which
                             the storage server is to register.
        @throws UnknownHostException If a stub cannot be created for the storage
                                     server because a valid address has not been
                                     assigned.
        @throws FileNotFoundException If the directory with which the server was
                                      created does not exist or is in fact a
                                      file.
        @throws RMIException If the storage server cannot be started, or if it
                             cannot be registered.
	 */
	public synchronized void start(String hostname, Registration naming_server)
			throws RMIException, UnknownHostException, FileNotFoundException
	{
		storageSkeleton.start();
		commandSkeleton.start();

		Storage storageStub = (Storage) Stub.create(Storage.class, storageSkeleton, hostname);
		Command commandStub = (Command) Stub.create(Command.class, commandSkeleton, hostname);

		Path[] duplicateFiles = naming_server.register(storageStub, commandStub, Path.list(root));
		for (Path p : duplicateFiles) {
			File currentFile = p.toFile(root);
			File parent = new File(currentFile.getParent());
			currentFile.delete();
			// Prune the parent file if it's empty
			while(!parent.equals(root)) {
				if (parent.list().length == 0) {
					parent.delete();
					parent =  new File(parent.getParent());
				} else 
					break;
			}
		}
	}

	/** Stops the storage server.

        <p>
        The server should not be restarted.
	 */
	public void stop() {
		storageSkeleton.stop();
		stopped(null);
		//throw new UnsupportedOperationException("not implemented");
	}

	/** Called when the storage server has shut down.

        @param cause The cause for the shutdown, if any, or <code>null</code> if
                     the server was shut down by the user's request.
	 */
	protected void stopped(Throwable cause)
	{

	}

	// The following methods are documented in Storage.java.
	@Override
	public synchronized long size(Path file) throws FileNotFoundException
	{
		File file2 =file.toFile(this.root);
		if(!file2.exists()) { 
			throw new FileNotFoundException("File not found");
		}
		if(file2.isDirectory()) {
			throw new FileNotFoundException("File is a directory");
		}
		return file2.length();
	}

	@Override
	public synchronized byte[] read(Path file, long offset, int length)
			throws FileNotFoundException, IOException
	{
		FileInputStream fis;
		int off = (int)offset;
		File file2 =file.toFile(this.root);
		if(!file2.exists()) 
			throw new FileNotFoundException("File not found");
		if(file2.isDirectory()) 
			throw new FileNotFoundException("File is a directory");
		if(length < 0 || offset < 0 || offset + length > file2.length())
			throw new IndexOutOfBoundsException();

		fis = new FileInputStream(file2);
		byte[] data = fis.readAllBytes();
		fis.read(data, off, length);  
		fis.close();
		return data;
	}

	@Override
	public synchronized void write(Path path, long offset, byte[] data)
			throws FileNotFoundException, IOException
	{
		FileOutputStream fos;
		File file = new File(this.root + path.toString());
		if(!file.exists())
			throw new FileNotFoundException("File does not exists ");
		if (file.isDirectory())
			throw new FileNotFoundException("File is a directory");
		if(offset < 0) 
			throw new IndexOutOfBoundsException();
		if(offset == 0) {
			fos = new FileOutputStream(file);
			fos.write(data, 0, data.length);
			fos.close();
		} else {
			offsetWrite(file, offset, data);
		}
	}

	 /**
     * Given an offset, Writes to a target some data
     *
     * @param target to write to
     * @param offset to start write
     * @param data to write
     * @throws IOException if error during write
     */
	private void offsetWrite(File file, long offset, byte[] data) throws IOException {

		FileInputStream fis = new FileInputStream(file);
		FileOutputStream fos;
		int off = (int)offset;
		int bytes = Math.min(fis.available(), off);
		byte[] readData = new byte[off + data.length];

		fis.read(readData, 0, bytes);

		for(int i = off; i < readData.length; i++)
			readData[i] = data[i-off];

		fos = new FileOutputStream(file);
		fos.write(readData, 0, readData.length);
		fos.close();
		fis.close();
	}

	// The following methods are documented in Command.java.
	@Override
	public synchronized boolean create(Path file)
	{
		File newFile;
		String[] components;

		if(file == null) 
			throw new NullPointerException();
		if(file.isRoot())
			return false;

		newFile = new File(this.root + file.toString());
		if(newFile.exists()){
			return false;
		}

		components = file.toString().substring(1).split("/");
		newFile = this.root;

		for(int i = 0; i < components.length; i++){

			newFile = new File(newFile, components[i]);
			if(!newFile.exists() && i != components.length-1) {
				newFile.mkdir();
			} else if(i == components.length-1) {
				try {
					newFile.createNewFile();
				} catch (IOException e) {
					System.out.println("IOException in StorageServer.java create method: "+e.getMessage());
				}
			}
		}

		return true;
		//throw new UnsupportedOperationException("not implemented");
	}

	@Override
	public synchronized boolean delete(Path path)
	{
		if(path.isRoot()) 
			return false;
		File delete = new File(this.root, path.toString());
		if(!delete.exists()) {
			return false;
		}
		if(delete.isFile()) {
			return delete.delete();
		}
		recursiveFileDelete(delete);
		return true;
	}

	/**
	 * This method deletes a file or folder
	 *
	 * @param toDelete file/folder to delete
	 */
	public void recursiveFileDelete(File toDelete) {
		for(File file : toDelete.listFiles()) {
			if(file == null) return;
			if(file.isDirectory()) {
				recursiveFileDelete(file);
			}
			file.delete();
		}
		toDelete.delete();
	}
}
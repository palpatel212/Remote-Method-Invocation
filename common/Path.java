package common;

import java.io.*;
import java.util.*;

/** Distributed filesystem paths.

    <p>
    Objects of type <code>Path</code> are used by all filesystem interfaces.
    Path objects are immutable.

    <p>
    The string representation of paths is a forward-slash-delimited sequence of
    path components. The root directory is represented as a single forward
    slash.

    <p>
    The colon (<code>:</code>) and forward slash (<code>/</code>) characters are
    not permitted within path components. The forward slash is the delimiter,
    and the colon is reserved as a delimiter for application use.
 */
@SuppressWarnings("serial")
public class Path implements Iterable<String>, Serializable
{
	private String root = "/";
	private String path_str;

	/** Creates a new path which represents the root directory. */
	public Path() {
		this.path_str = root;
	}

	/** Creates a new path by appending the given component to an existing path.

        @param path The existing path.
        @param component The new component.
        @throws IllegalArgumentException If <code>component</code> includes the
                                         separator, a colon, or
                                         <code>component</code> is the empty
                                         string.
	 */
	public Path(Path path, String component) {
		if(component.contains(":")) {
			throw new IllegalArgumentException("Component contains : ");
		}
		else if(component.contains("/")) {
			throw new IllegalArgumentException("Component contains / ");
		}
		else if(component.isEmpty()) {
			throw new IllegalArgumentException("Component is empty ");
		}
		else {
			String newpath = null;
			if(path.isRoot()) {
				newpath = path.toString() + component;
			} else {
				newpath = path.toString() + "/" + component;
			}

			this.path_str = newpath;
		}
	}
	/** Creates a new path from a path string.

        <p>
        The string is a sequence of components delimited with forward slashes.
        Empty components are dropped. The string must begin with a forward
        slash.

        @param path The path string.
        @throws IllegalArgumentException If the path string does not begin with
                                         a forward slash, or if the path
                                         contains a colon character.
	 */
	public Path(String path) {
		if(path.equals("")) {
			throw new IllegalArgumentException("path is empty");
		}
		if(!path.startsWith("/")) {
			throw new IllegalArgumentException("Path does not start with forward slash");
		}
		if(path.contains(":")) {
			throw new IllegalArgumentException("Path contains :");
		}
		this.path_str = "";
		for(String component : path.split("/")) {
			if(!component.trim().equals("")){
				path_str = path_str.concat("/");
				path_str = path_str.concat(component.trim());
			}
		}
		if(path_str.equals("")) {
			path_str = "/";
		}
		//throw new UnsupportedOperationException("not implemented");
	}

	/** Returns an iterator over the components of the path.

        <p>
        The iterator cannot be used to modify the path object - the
        <code>remove</code> method is not supported.

        @return The iterator.
	 */
	@Override
	public Iterator<String> iterator()
	{
		return new PathIter();
		//throw new UnsupportedOperationException("not implemented");
	}

	class PathIter implements Iterator<String> {
		List<String> components = new ArrayList<>();
		Iterator<String> iterator;

		public PathIter() {
			for(String element : path_str.split("/")) {
				if(!element.equals(""))
					components.add(element.trim());
			}
			iterator = components.iterator();
		}
		@Override
		public boolean hasNext() {
			return iterator.hasNext();
		}
		@Override
		public String next() {
			return iterator.next();
		}
		@Override
		public void remove() {
			throw new UnsupportedOperationException("not implemented");
		}
	}


	/** Lists the paths of all files in a directory tree on the local
        filesystem.

        @param directory The root directory of the directory tree.
        @return An array of relative paths, one for each file in the directory
                tree.
        @throws FileNotFoundException If the root directory does not exist.
        @throws IllegalArgumentException If <code>directory</code> exists but
                                         does not refer to a directory.
	 */
	public static Path[] list(File directory) throws FileNotFoundException
	{
		if(!directory.exists()) {
			throw new FileNotFoundException("Directory does not exist");
		}
		if(!directory.isDirectory()) {
			throw new IllegalArgumentException("File is not a directory");
		}
		return getPaths(directory, new ArrayList<Path>(), directory.getAbsolutePath().length());
		//throw new UnsupportedOperationException("not implemented");
	}
	

	public static Path[] getPaths(File directory, List<Path> paths, int Length){
		for(File file : directory.listFiles()) {
			if(file.isDirectory()) {
				getPaths(file, paths, Length);
			} else {
				String filePath = file.getAbsolutePath().substring(Length);
				filePath = filePath.replaceAll("\\\\", "/");
				paths.add(new Path(filePath));
			}
		}
		return paths.toArray(new Path[0]);
	}

	public Path add(String path) {
		return new Path(path);
	}

	/** Determines whether the path represents the root directory.

        @return <code>true</code> if the path does represent the root directory,
                and <code>false</code> if it does not.
	 */
	public boolean isRoot() {
		return this.path_str.equals(root);
		//throw new UnsupportedOperationException("not implemented");
	}

	/** Returns the path to the parent of this path.

        @throws IllegalArgumentException If the path represents the root
                                         directory, and therefore has no parent.
	 */
	public Path parent() {
		if(this.path_str.equals(root)) {
			throw new IllegalArgumentException("Path is root and therefore has no parent");
		}
		String parentPath = "";
		String[] components = this.path_str.split("/");
		for(int i = 0 ; i < components.length - 1 ; i++) {
			parentPath = parentPath + "/" + components[i].trim();
		}
		return new Path(parentPath);
		//throw new UnsupportedOperationException("not implemented");
	}

	/** Returns the last component in the path.

        @throws IllegalArgumentException If the path represents the root
                                         directory, and therefore has no last
                                         component.
	 */
	public String last() {
		if(isRoot())
			throw new IllegalArgumentException("Path is the root directory and thus has no components");
		return this.path_str.split("/")[this.path_str.split("/").length-1];
	}

	/** Determines if the given path is a subpath of this path.

        <p>
        The other path is a subpath of this path if is a prefix of this path.
        Note that by this definition, each path is a subpath of itself.

        @param other The path to be tested.
        @return <code>true</code> If and only if the other path is a subpath of
                this path.
	 */
	public boolean isSubpath(Path other) {
		return this.path_str.contains(other.path_str);
	}

	/** Converts the path to <code>File</code> object.

        @param root The resulting <code>File</code> object is created relative
                    to this directory.
        @return The <code>File</code> object.
	 */
	public File toFile(File root) { 
		File l_file = new File(this.path_str);
		if (l_file.isAbsolute())
			return l_file;
		if (root == null)
			return null;
		return new File(root, this.path_str);
	}

	/** Compares two paths for equality.

        <p>
        Two paths are equal if they share all the same components.

        @param other The other path.
        @return <code>true</code> if and only if the two paths are equal.
	 */
	@Override
	public boolean equals(Object other) {
		return this.path_str.equals(((Path)other).path_str);
	}

	/** Returns the hash code of the path. */
	@Override
	public int hashCode() {
		return this.path_str.hashCode();
	}

	/** Converts the path to a string.

        <p>
        The string may later be used as an argument to the
        <code>Path(String)</code> constructor.

        @return The string representation of the path.
	 */
	@Override
	public String toString() {
		return this.path_str;
	}
}

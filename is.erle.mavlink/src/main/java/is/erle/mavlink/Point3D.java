package is.erle.mavlink;

public class Point3D
{
	private double x;
	private double y;
	private double z;
	private int hash = 0;

	public final double getX()
	{
		return x;
	}

	public final double getY()
	{
		return y;
	}

	public final double getZ()
	{
		return z;
	}

	public Point3D(double x, double y, double z)
	{
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public Point3D add(Point3D point3D)
	{
		return add(point3D.getX(), point3D.getY(), point3D.getZ());
	}

	public Point3D add(double x, double y, double z)
	{
		return new Point3D(getX() + x, getY() + y, getZ() + z);
	}

	public double distance(double x1, double y1, double z1)
	{
		double a = getX() - x1;
		double b = getY() - y1;
		double c = getZ() - z1;
		return Math.sqrt(a * a + b * b + c * c);
	}

	public double distance(Point3D point3D)
	{
		return distance(point3D.getX(), point3D.getY(), point3D.getZ());
	}

	public Point3D subtract(double x, double y, double z)
	{
		return new Point3D(getX() - x, getY() - y, getZ() - z);
	}

	public Point3D subtract(Point3D point3D)
	{
		return subtract(point3D.getX(), point3D.getY(), point3D.getZ());
	}

	public Point3D multiply(double constant)
	{
		return new Point3D(getX() * constant, getY() * constant, getZ() * constant);
	}

	public Point3D normalize()
	{
		final double mag = magnitude();

		if (mag == 0.0)
		{
			return new Point3D(0.0, 0.0, 0.0);
		}

		return new Point3D(getX() / mag, getY() / mag, getZ() / mag);
	}

	public Point3D midpoint(double x, double y, double z)
	{
		return new Point3D(x + (getX() - x) / 2.0, y + (getY() - y) / 2.0, z
				+ (getZ() - z) / 2.0);
	}

	public Point3D midpoint(Point3D point3D)
	{
		return midpoint(point3D.getX(), point3D.getY(), point3D.getZ());
	}

	public double angle(double x, double y, double z)
	{
		final double ax = getX();
		final double ay = getY();
		final double az = getZ();

		final double delta = (ax * x + ay * y + az * z)
				/ Math.sqrt((ax * ax + ay * ay + az * az)
						* (x * x + y * y + z * z));

		if (delta > 1.0)
		{
			return 0.0;
		}
		if (delta < -1.0)
		{
			return 180.0;
		}

		return Math.toDegrees(Math.acos(delta));
	}

	public double angle(Point3D point3D)
	{
		return angle(point3D.getX(), point3D.getY(), point3D.getZ());
	}

	public double angle(Point3D point3D1, Point3D point3D2)
	{
		final double x = getX();
		final double y = getY();
		final double z = getZ();

		final double ax = point3D1.getX() - x;
		final double ay = point3D1.getY() - y;
		final double az = point3D1.getZ() - z;
		final double bx = point3D2.getX() - x;
		final double by = point3D2.getY() - y;
		final double bz = point3D2.getZ() - z;

		final double delta = (ax * bx + ay * by + az * bz)
				/ Math.sqrt((ax * ax + ay * ay + az * az)
						* (bx * bx + by * by + bz * bz));

		if (delta > 1.0)
		{
			return 0.0;
		}
		if (delta < -1.0)
		{
			return 180.0;
		}

		return Math.toDegrees(Math.acos(delta));
	}

	public double magnitude()
	{
		final double x = getX();
		final double y = getY();
		final double z = getZ();

		return Math.sqrt(x * x + y * y + z * z);
	}

	public double dotProduct(double x, double y, double z)
	{
		return getX() * x + getY() * y + getZ() * z;
	}

	public double dotProduct(Point3D point3D)
	{
		return dotProduct(point3D.getX(), point3D.getY(), point3D.getZ());
	}

	public Point3D crossProduct(double x, double y, double z)
	{
		final double ax = getX();
		final double ay = getY();
		final double az = getZ();

		return new Point3D(ay * z - az * y, az * x - ax * z, ax * y - ay * x);
	}

	public Point3D crossProduct(Point3D point3D)
	{
		return crossProduct(point3D.getX(), point3D.getY(), point3D.getZ());
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == this)
			return true;
		if (obj instanceof Point3D)
		{
			Point3D other = (Point3D) obj;
			return getX() == other.getX() && getY() == other.getY()
					&& getZ() == other.getZ();
		}
		else
			return false;
	}

	@Override
	public int hashCode()
	{
		if (hash == 0)
		{
			long bits = 7L;
			bits = 31L * bits + Double.doubleToLongBits(getX());
			bits = 31L * bits + Double.doubleToLongBits(getY());
			bits = 31L * bits + Double.doubleToLongBits(getZ());
			hash = (int) (bits ^ (bits >> 32));
		}
		return hash;
	}

	@Override
	public String toString()
	{
		return "Point3D [x = " + getX() + ", y = " + getY() + ", z = " + getZ()
				+ "]";
	}

}

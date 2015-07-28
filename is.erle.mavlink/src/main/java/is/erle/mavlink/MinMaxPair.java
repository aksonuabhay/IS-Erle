package is.erle.mavlink;

public class MinMaxPair<T>
{
	public T min;
	public T max;

	public MinMaxPair(T min, T max)
	{
		this.min = min;
		this.max = max;
	}
	
	public T getMin()
	{
		return min;
	}
	
	public T getMax()
	{
		return max;
	}
	
	public void update(T min, T max)
	{
		this.min = min;
		this.max = max;
	}
}

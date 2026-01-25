package com.codexaga;

public final class Star
{
    private double x;
    private double y;
    private final double speed;
    private final double radius;

    public Star(double x, double y, double speed, double radius)
    {
        this.x = x;
        this.y = y;
        this.speed = speed;
        this.radius = radius;
    }

    public double getX()
    {
        return x;
    }

    public double getY()
    {
        return y;
    }

    public double getSpeed()
    {
        return speed;
    }

    public double getRadius()
    {
        return radius;
    }

    public void update(double delta)
    {
        y += speed * delta;
        if (y > GameConfig.HEIGHT + radius)
        {
            y = -radius;
        }
    }

    public void setX(double x)
    {
        this.x = x;
    }

    public void setY(double y)
    {
        this.y = y;
    }
}

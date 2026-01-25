package com.codexaga;

public final class InputState
{
    private boolean left;
    private boolean right;
    private boolean fire;
    private boolean pausePressed;
    private boolean restartPressed;
    private boolean startPressed;

    public void setLeft(boolean pressed)
    {
        left = pressed;
    }

    public void setRight(boolean pressed)
    {
        right = pressed;
    }

    public void setFire(boolean pressed)
    {
        fire = pressed;
    }

    public boolean isLeft()
    {
        return left;
    }

    public boolean isRight()
    {
        return right;
    }

    public boolean isFire()
    {
        return fire;
    }

    public void requestPause()
    {
        pausePressed = true;
    }

    public void requestRestart()
    {
        restartPressed = true;
    }

    public void requestStart()
    {
        startPressed = true;
    }

    public boolean consumePause()
    {
        boolean value = pausePressed;
        pausePressed = false;
        return value;
    }

    public boolean consumeRestart()
    {
        boolean value = restartPressed;
        restartPressed = false;
        return value;
    }

    public boolean consumeStart()
    {
        boolean value = startPressed;
        startPressed = false;
        return value;
    }
}

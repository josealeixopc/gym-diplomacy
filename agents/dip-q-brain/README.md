# dip-q-brain

This directory will be used to run the agents for the Gym Diplomacy environment.

## Installing

To install all the necessary packages to run this, simply use `pipenv` and install what's on the `Pipfile`. Because some of the `setup.py` files are outdated, **simply using `pipenv install` will give an error when creating the `Pipfile.lock` file**. Therefore, you must run the command:

```bash
pipenv install --skip-lock  # skips the creation of the lock file
```

## Running

If running locally, you might want to spare your CPU. Use the [`nice`](https://www.computerhope.com/unix/unice.htm) command before the command to run the agent.
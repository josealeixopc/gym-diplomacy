# gym-diplomacy

This folder contains the adapted environment of Diplomacy.

It is important to note that it **does not** follow the Open AI Gym environments framework.

In an Open AI Gym environment, the action is **taken by the agent whenever he wants to act**. However, because this is a port of the BANDANA framework for Diplomacy, a BANDANA agent can only act when the BANDANA game says so, we have **the environment deciding when the agent shall act**.

This "pseudo-environment" is still based on Open AI Gym and it uses its Spaces, but it cannot be used as a regular Open AI Gym environment.

Instead of an agent building up the environment, the environment builds agents and calls them to act.

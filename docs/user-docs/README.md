# Seed Project Documentation

## How to build and view

First, make sure you are authenticated with the GitHub container repository:

```bash
docker login ghcr.io
```

Then, run:

```bash
docker compose up --build
```

or 

```bash
make serve
```

Note that you will have to `ctrl-c` and run the command again if you want to see changes to the documentation.

To verify link correctness, run `make htmltest`.

(Note that you do not need to be in this module's directory to run the commands -- if you are in the top-level
directory, you can run e.g. `make -C docs/user-docs serve`
or `docker compose -f docs/user-docs/docker-compose.yml up --build` instead).

## How it works

This directory provides documentation for our website (https://docs.core.noumenadigital.com/). When the contents of the
repository which you are in right now get merged to master, the `documentation` repo will pull them in through a 
submodule and copy over the markdown contents.

Note that when serving the docs locally in this repo, content which is *not* provided by this repo will be incorrectly
rendered in some cases (most snippets will be empty, for example).

Please refer to the README in the `documentation` repo for further information.

## Snippets

[PyMdown snippets](https://facelessuser.github.io/pymdown-extensions/extensions/snippets/) are used to render code
snippets. Simply specify the file path as `seed/<path in seed repo>`.

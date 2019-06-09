#!/bin/bash

clojure -Sdeps "{:paths [\"src\" \"resources\" \"scripts\"]}" -m build

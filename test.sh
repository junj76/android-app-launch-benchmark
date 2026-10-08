#/bin/bash

./gradlew run --args="-t 0 -p no_swap -a 0"
./gradlew run --args="-t 0 -p no_swap -a 1"
./gradlew run --args="-t 0 -p no_swap -a 2"

./gradlew run --args="-t 1 -p only_zram -a 0"
./gradlew run --args="-t 1 -p only_zram -a 1"
./gradlew run --args="-t 1 -p only_zram -a 2"

./gradlew run --args="-t 2 -p flash_swap -a 0"
./gradlew run --args="-t 2 -p flash_swap -a 1"
./gradlew run --args="-t 2 -p flash_swap -a 2"

./gradlew run --args="-t 3 -p zswap -a 0"
./gradlew run --args="-t 3 -p zswap -a 1"
./gradlew run --args="-t 3 -p zswap -a 2"


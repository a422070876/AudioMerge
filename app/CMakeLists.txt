

cmake_minimum_required(VERSION 3.4.1)

aux_source_directory(src/main/cpp/lamemp3 lamemp3)

add_library(
             native-lib
             SHARED
             src/main/cpp/native-lib.cpp
              ${lamemp3}
              )


find_library(
              log-lib
              log )


target_link_libraries(
                       native-lib
                       ${log-lib} )
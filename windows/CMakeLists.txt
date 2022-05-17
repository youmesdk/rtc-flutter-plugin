cmake_minimum_required(VERSION 3.14)
set(PROJECT_NAME "youme_rtc_engine")
project(${PROJECT_NAME} LANGUAGES CXX)

# This value is used when generating builds using this plugin, so it must
# not be changed
set(PLUGIN_NAME "youme_rtc_engine_plugin")

# download sdk
set(YOUME_SDK_DOWNLOAD_NAME "YoumeVideoSDK_3.4.1.1025_windows")
set(YOUME_SDK_VERSION "3.4.1.1025")

# Add this project's cmake/ directory to the module path.
set(CMAKE_MODULE_PATH ${CMAKE_MODULE_PATH} "${CMAKE_CURRENT_SOURCE_DIR}/cmake")

set(YOUME_DOWNLOAD_PATH "${CMAKE_CURRENT_SOURCE_DIR}/third_party/youme")

set(YOUME_SDK_PATH "${YOUME_DOWNLOAD_PATH}/${YOUME_SDK_DOWNLOAD_NAME}/x64")
set(YOUME_SDK_BIN_PATH "${YOUME_SDK_PATH}/Release")
set(RTC_SDK_PATH "${YOUME_DOWNLOAD_PATH}/${YOUME_SDK_DOWNLOAD_NAME}/RTC")
set(RTC_SDK_BIN_PATH "${RTC_SDK_PATH}/YoumeVideoSDK_3.4.1.1025_windows/libs/x86_64")

if(NOT EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/.plugin_dev")
    # Download and extract the SDK binary distribution (executes DownloadSDK.cmake).
    include(DownloadSDK)
    DownloadSDK("windows" "${YOUME_SDK_VERSION}" "${YOUME_DOWNLOAD_PATH}")   
endif()


add_library(${PLUGIN_NAME} SHARED
  "youme_rtc_engine_plugin.cpp"
)
apply_standard_settings(${PLUGIN_NAME})
set_target_properties(${PLUGIN_NAME} PROPERTIES
  CXX_VISIBILITY_PRESET hidden)
target_compile_definitions(${PLUGIN_NAME} PRIVATE FLUTTER_PLUGIN_IMPL)
target_include_directories(${PLUGIN_NAME} INTERFACE
  "${CMAKE_CURRENT_SOURCE_DIR}/include")
target_link_libraries(${PLUGIN_NAME} PRIVATE flutter flutter_wrapper_plugin)

# List of absolute paths to libraries that should be bundled with the plugin
set(youme_rtc_engine_bundled_libraries
  ""
  PARENT_SCOPE
)

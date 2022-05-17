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

set(YOUME_SDK_INC_PATH "${YOUME_DOWNLOAD_PATH}/${YOUME_SDK_DOWNLOAD_NAME}/include")
set(YOUME_SDK_BIN_PATH "${YOUME_DOWNLOAD_PATH}/${YOUME_SDK_DOWNLOAD_NAME}/win64/release")

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
  "${CMAKE_CURRENT_SOURCE_DIR}/include"
  PRIVATE
  "${YOUME_SDK_INC_PATH}"
  )
target_link_libraries(${PLUGIN_NAME} PRIVATE flutter flutter_wrapper_plugin ${YOUME_SDK_BIN_PATH}/lib/youme_voice_engine.lib)

# List of absolute paths to libraries that should be bundled with the plugin
set(youme_rtc_engine_bundled_libraries
  "${YOUME_SDK_BIN_PATH}/dll/avcodec-57.dll"
  "${YOUME_SDK_BIN_PATH}/dll/avdevice-57.dll"
  "${YOUME_SDK_BIN_PATH}/dll/avfilter-6.dll"
  "${YOUME_SDK_BIN_PATH}/dll/avformat-57.dll"
  "${YOUME_SDK_BIN_PATH}/dll/avutil-55.dll"
  "${YOUME_SDK_BIN_PATH}/dll/D3DCompiler_43.dll"
  "${YOUME_SDK_BIN_PATH}/dll/d3dx10_43.dll"
  "${YOUME_SDK_BIN_PATH}/dll/libscreen.dll"
  "${YOUME_SDK_BIN_PATH}/dll/libx264-152.dll"
  "${YOUME_SDK_BIN_PATH}/dll/MediaApi.dll"
  "${YOUME_SDK_BIN_PATH}/dll/postproc-54.dll"
  "${YOUME_SDK_BIN_PATH}/dll/SDL2.dll"
  "${YOUME_SDK_BIN_PATH}/dll/swresample-2.dll"
  "${YOUME_SDK_BIN_PATH}/dll/swscale-4.dll"
  "${YOUME_SDK_BIN_PATH}/dll/youme_voice_engine.dll"
  PARENT_SCOPE
)

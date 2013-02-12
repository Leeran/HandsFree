################################################################################
# Automatically-generated file. Do not edit!
################################################################################

# Add inputs and outputs from these tool invocations to the build variables 
CPP_SRCS += \
../jni/motion_averager.cpp 

OBJS += \
./jni/motion_averager.o 

CPP_DEPS += \
./jni/motion_averager.d 


# Each subdirectory must supply rules for building sources it contributes
jni/motion_averager.o: ../jni/motion_averager.cpp
	@echo 'Building file: $<'
	@echo 'Invoking: Cross G++ Compiler'
	/Users/leeranraphaely/android-ndk-r8d/ndk-build -O0 -g3 -Wall -c -fmessage-length=0 -MMD -MP -MF"$(@:%.o=%.d)" -MT"jni/motion_averager.d" -o "$@" "$<"
	@echo 'Finished building: $<'
	@echo ' '



/*
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



package com.netflix.gradle.plugins.cloud.asgard.services

import retrofit.client.Response
import retrofit.http.*

interface AsgardClient {
  // why, oh why doesn't asgard accept JSON here?
  @POST("/{region}/autoScaling/save")
  @FormUrlEncoded
  Response deploy(
      @Path("region") String region,
      @Field("region") String region1,
      @Field("appName") String app,
      @Field("stack") String stack,
      @Field("min") int min,
      @Field("desiredCapacity") int desiredCapacity,
      @Field("max") int max,
      @Field("healthCheckGracePeriod") int healthCheckGracePeriod,
      @Field("terminationPolicy") String terminationPolicy,
      @Field("subnetPurpose") String subnetPurpose,
      @Field("selectedZones") List<String> selectedZones,
      @Field("imageId") String imageId,
      @Field("keyName") String keyName,
      @Field("selectedSecurityGroups") List<String> selectedSecurityGroups,
      @Field("instanceType") String instanceType, @Field("iamInstanceProfile") String iamInstanceProfile)

  @GET("/application/show/{appName}.json")
  Response getApp(@Path("appName") String appName)

  @POST("/application/save")
  @FormUrlEncoded
  Response createApp(
      @Field("name") String name,
      @Field("group") String group,
      @Field("type") String type,
      @Field("description") String description,
      @Field("owner") String owner,
      @Field("email") String email, @Field("monitorBucketType") String monitorBucketType, @Field("tags") String tags)

  @POST("/{region}/deployment/start")
  Response deployNext(@Path("region") String region, @Body Map payload)

}
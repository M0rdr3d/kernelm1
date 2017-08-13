package com.latamautos.kernel.scala.util

import java.net.InetAddress
import com.latamautos.kernel.scala.rest_api.Config
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest
import com.amazonaws.services.ec2.AmazonEC2Client
import com.amazonaws.services.ec2.model.{DescribeInstancesRequest, Instance, InstanceStateName}
import scala.collection.JavaConversions._

class AwsEc2(scaling: AmazonAutoScalingClient, ec2: AmazonEC2Client) extends Config {
  val autoScallingGroup: String = amazonConfig.getString("autoscaling-group")
  def siblingIps: List[String]  = groupInstanceIds(autoScallingGroup) map instanceFromId collect {
    case instance if isRunning(instance) => instance.getPrivateIpAddress
  }
  val localhost = InetAddress.getLocalHost
  def currentIp:String = localhost.getHostAddress

  val isRunning: Instance => Boolean = _.getState.getName == InstanceStateName.Running.toString

  private def instanceFromId(id: String): Instance = {
    val result = ec2 describeInstances new DescribeInstancesRequest {
      setInstanceIds(id :: Nil)
    }
    result.getReservations.head.getInstances.head
  }

  private def groupInstanceIds(groupName: String) = {
    val result = scaling describeAutoScalingGroups new DescribeAutoScalingGroupsRequest {
      setAutoScalingGroupNames(groupName :: Nil)
    }
    result.getAutoScalingGroups.head.getInstances.toList map (_.getInstanceId)
  }
}
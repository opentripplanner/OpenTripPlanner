/**
 * What is this package doing here?
 *
 * In 2013, significant improvements were made to OTP as part of a precommercial procurement project
 * in The Netherlands called MMRI ("MultiModale ReisInformatie" => "multimodal travel information").
 * This project is itself part of a larger project called "Better Benutten" => "better utilization".
 * Most effort concentrated on the implementation of GTFS-RT updates and related improvements to the
 * architecture of OTP. Additionally, a testing module was developed to verify that all the planners
 * that were involved in the project (not just OTP) met a minimum set of requirements. OTP was first
 * to pass all tests, ahead of two different solutions. Unfortunately, having two sets of tests does
 * not make it simpler to continuously verify that OTP still functions correctly, which is why these
 * MMRI tests have now been added to OTP's own test suite. These versions are intended to be a close
 * approximation of reality, but several minor shortcuts have been taken, like applying trip updates
 * directly to the graph instead of going through the thread-safe graph writer framework. Given that
 * thread-safety is a technical issue and not a functional one, this is considered to be acceptable.
 */
package org.opentripplanner.mmri;
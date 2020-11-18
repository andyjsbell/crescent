/* eslint-disable */
// this is an auto generated file. This will be overwritten

export const getState = /* GraphQL */ `
  query GetState($id: ID!) {
    getState(id: $id) {
      id
      lfdId
      upTime
      OPS
      Front
      HDMI1
      HDMI2
      HDMI3
      DP
      VGA
      usb
      brightness
      createdAt
      updatedAt
    }
  }
`;
export const listStates = /* GraphQL */ `
  query ListStates(
    $filter: ModelStateFilterInput
    $limit: Int
    $nextToken: String
  ) {
    listStates(filter: $filter, limit: $limit, nextToken: $nextToken) {
      items {
        id
        lfdId
        upTime
        OPS
        Front
        HDMI1
        HDMI2
        HDMI3
        DP
        VGA
        usb
        brightness
        createdAt
        updatedAt
      }
      nextToken
    }
  }
`;

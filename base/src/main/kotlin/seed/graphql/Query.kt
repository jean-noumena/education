package seed.graphql

fun getProtocolIdQuery(prototypeId: String) =
    """
        {
          protocolStates(
            filter: { protoRefId: { equalTo: "$prototypeId" } }
          ) {
            totalCount
            nodes {
              protocolId
            }
          }
        }
        """

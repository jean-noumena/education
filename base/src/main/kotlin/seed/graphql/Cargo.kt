package seed.graphql

data class Node(val protocolId: String)
data class ProtocolStates(val totalCount: Int, val nodes: List<Node>)
data class Data(val protocolStates: ProtocolStates)
data class GraphQlResponse(val data: Data)

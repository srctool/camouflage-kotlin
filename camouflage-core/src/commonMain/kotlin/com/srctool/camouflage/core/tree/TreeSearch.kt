package com.srctool.camouflage.core.tree

// The result of a speed search over a tree: the nodes to show, the ancestors to open, and what matched where.
internal class TreeSearch<T>(
    val roots: List<CamoTreeNode<T>>,
    val open: Set<Any>,
    val hits: Map<Any, List<IntRange>>,
)

/**
 * Searches [roots]' loaded nodes with [match] (null: no match). With [filter] the result keeps the nodes that
 * match and the ancestors of those that do, and opens those ancestors; without it the tree is kept whole and
 * only [TreeSearch.hits] says what matched.
 */
internal fun <T> searchTree(
    roots: List<CamoTreeNode<T>>,
    label: (T) -> String,
    filter: Boolean,
    match: (String) -> List<IntRange>?,
): TreeSearch<T> {
    val open = HashSet<Any>()
    val hits = HashMap<Any, List<IntRange>>()
    fun visit(node: CamoTreeNode<T>): CamoTreeNode<T>? {
        val own = match(label(node.value))
        if (own != null) hits[node.key] = own
        val kept = node.children?.mapNotNull { visit(it) }.orEmpty()
        val below = kept.isNotEmpty()
        if (below) open += node.key
        return when {
            !filter -> node
            below -> node.copy(children = kept)
            own != null -> node
            else -> null
        }
    }
    val shown = roots.mapNotNull { visit(it) }
    return TreeSearch(shown, if (filter) open else emptySet(), hits)
}

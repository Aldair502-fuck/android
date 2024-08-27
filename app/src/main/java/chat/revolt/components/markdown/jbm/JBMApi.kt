package chat.revolt.components.markdown.jbm

import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser

@RequiresOptIn(message = "This API is experimental and has many TODOs.")
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
annotation class JBM

@JBM
object JBMApi {
    fun parse(src: String): ASTNode {
        return MarkdownParser(GFMFlavourDescriptor()).buildMarkdownTreeFromString(src)
    }
}
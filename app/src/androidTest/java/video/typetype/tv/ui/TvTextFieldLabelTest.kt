package video.typetype.tv.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import video.typetype.tv.ui.auth.TvTextField

class TvTextFieldLabelTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theFieldIsAnnouncedWithItsLabel() {
        rule.setContent { TvTextField(value = "https://example.test", onValueChange = {}, label = "Instance address") }
        val labelled = SemanticsMatcher("has the label as its description") {
            it.config.getOrNull(SemanticsProperties.ContentDescription)?.contains("Instance address") == true
        }
        rule.onNode(labelled).assertIsDisplayed()
    }
}

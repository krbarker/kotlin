fun foo(f: (String) -> Unit) {}
fun bar(s: String) {}

fun test() {
    foo {
        <caret>foo {
            bar(it)
        }
    }
}
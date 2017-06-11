import com.hadihariri.markcode.OutputVerifier
import func.exkt.main as func_exkt


fun main(args: Array<String>) {
    val verifier = OutputVerifier()
    verifier.verifySample(::func_exkt, "samples-out/func/1_HelloWorld.txt", "func.adoc:8")
    verifier.report()
}

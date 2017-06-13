import com.hadihariri.markcode.OutputVerifier
import helloworld.exkt.main as helloworld_exkt


fun main(args: Array<String>) {
    val verifier = OutputVerifier()
    verifier.verifySample(::helloworld_exkt, "samples-out/helloworld/1_HelloWorld.txt", "helloworld.adoc:8")
    verifier.report()
}

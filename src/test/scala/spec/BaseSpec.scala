package spec


import org.scalatest._
import org.scalatest.mock.MockitoSugar

/**
  * Created by sebastian on 3/15/16.
  */
trait BaseSpec extends WordSpecLike with Matchers with BeforeAndAfterEach with BeforeAndAfterAll
  with MockitoSugar with GivenWhenThen {

}